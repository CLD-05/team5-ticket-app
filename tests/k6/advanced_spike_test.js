import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';

/**
 * [실무형 부하테스트 시나리오]
 * 
 * 주요 개선점:
 * 1. setup() 생명주기를 이용한 사전 로그인 처리 (테스트 중 BCrypt CPU 병목 제거)
 * 2. 가상 유저(VU)별 고유 좌석 ID 분산 매핑 (중복 선점 튕김 방지 및 비동기 SQS/워커 부하 전달)
 * 3. 환경 변수를 통한 설정 제어 (__ENV)
 * 4. 세밀한 에러 체크 및 상세 메트릭 수집
 */

// 1. 설정값 정의 (환경 변수 적용 가능)
const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080/api/v1';
const MAX_SEATS = parseInt(__ENV.MAX_SEATS || '10000'); // 테스트할 총 좌석 수
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'password123';

// 2. 테스트 스케줄링 설정 (Spike Test)
export const options = {
    scenarios: {
        advanced_spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 100 },   // 웜업 단계
                { duration: '1m', target: 100 },    // 평시 유지
                { duration: '10s', target: 1000 },  // 순간 트래픽 스파이크 (실제 타겟으로 조절 가능)
                { duration: '2m', target: 1000 },   // 스파이크 부하 유지 (오토스케일링 및 SQS 적체 관측)
                { duration: '30s', target: 0 },     // 정리
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95%의 요청은 500ms 이내에 처리되어야 함
        http_req_failed: ['rate<0.01'],   // 에러율 1% 미만 유지
    },
};

// 3. 테스트 시작 전 딱 1번만 실행되는 초기화 함수 (Setup Stage)
// 여기서 테스트에 사용할 유저들의 JWT 토큰을 미리 발급받아 메모리에 적재합니다.
export function setup() {
    console.log('--- [Setup] 사전 로그인 및 토큰 발급 시작 ---');
    const tokens = [];
    
    // DB에 사전에 생성된 사용자 정보를 바탕으로 로그인 요청을 보냅니다.
    // (환경 변수 MAX_VUS에 따라 연동되며, 미지정 시 기본 1000명)
    const totalTestUsers = Math.min(10000, parseInt(__ENV.MAX_VUS || '1000')); 
    
    for (let i = 1; i <= totalTestUsers; i++) {
        const username = `user${i}`;
        const loginParams = {
            headers: { 'Content-Type': 'application/json' },
        };
        const payload = JSON.stringify({
            username: username,
            password: TEST_PASSWORD
        });
        
        const loginRes = http.post(`${BASE_URL}/auth/login`, payload, loginParams);
        
        if (loginRes.status === 200) {
            const token = loginRes.json().accessToken;
            tokens.push(token);
        } else {
            console.error(`[Setup Error] 유저 로그인 실패: ${username} (Status: ${loginRes.status})`);
        }
        
        // 너무 빠른 요청으로 Setup 중 서버에 부하가 걸리지 않도록 짧은 휴식
        if (i % 100 === 0) {
            sleep(0.1);
        }
    }
    
    console.log(`--- [Setup] 로그인 완료. 성공한 토큰 개수: ${tokens.length} ---`);
    
    // 반환된 데이터는 아래의 default function(VU)들의 파라미터로 전달됩니다.
    return { tokens: tokens };
}

// 4. 실제 병렬로 부하를 발생시키는 메인 함수 (VU Stage)
// setup()에서 리턴한 데이터(data.tokens)를 주입받아 사용합니다.
export default function (data) {
    const tokens = data.tokens;
    if (!tokens || tokens.length === 0) {
        console.error('사용 가능한 인증 토큰이 존재하지 않습니다.');
        return;
    }

    // 1단계. 내 가상 유저 번호에 매핑되는 인증 토큰 꺼내기 (로그인 과정 생략)
    // VU ID를 이용해 토큰 배열에서 고유 토큰을 할당받습니다.
    const myTokenIndex = (exec.vu.idInInstance - 1) % tokens.length;
    const token = tokens[myTokenIndex];
    
    const params = {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };

    // 2단계. 대기열 참가
    const joinRes = http.post(`${BASE_URL}/queue/join`, null, params);
    const joinCheck = check(joinRes, { 'join queue successful': (r) => r.status === 200 });
    if (!joinCheck) return;
    sleep(1);

    // 3단계. 대기열 대기 (ALLOW 판정이 날 때까지 2초마다 최대 10번 조회)
    let allowed = false;
    for (let i = 0; i < 10; i++) {
        const statusRes = http.get(`${BASE_URL}/queue/status`, params);
        if (statusRes.json().status === 'ACTIVE' || statusRes.json().canEnter === true) {
            allowed = true;
            break;
        }
        sleep(2);
    }

    if (!allowed) return; // 제한시간 내 대기열을 통과하지 못하면 퇴장

    // 4단계. 좌석 임시 선택 (락 획득)
    // [실무 튜닝] 가상 유저별로 각자 다른 좌석 번호를 공격하게 분산시킵니다.
    // 동시성 충돌 강도를 테스트하고 싶다면 분산 범위를 좁히고, 대량 처리를 테스트하고 싶다면 넓힙니다.
    const seatId = (exec.vu.idInInstance % MAX_SEATS) + 1; 
    
    const holdRes = http.post(`${BASE_URL}/seats/${seatId}/hold`, null, params);
    const holdCheck = check(holdRes, { 'hold seat successful': (r) => r.status === 200 });
    
    // 만약 이미 다른 유저가 이 좌석을 점유했다면 퇴장합니다.
    if (!holdCheck) return; 

    // 5단계. 예매 확정 요청 (비동기 처리 시작 - SQS 전송)
    // 최종 결제 및 예약 완료 단계는 SQS 큐로 전송하여 비동기로 백엔드 워커에서 처리하도록 유도합니다.
    const bookingRes = http.post(`${BASE_URL}/bookings?seatId=${seatId}`, null, params);
    check(bookingRes, { 'booking requested successful': (r) => r.status === 202 });
}
