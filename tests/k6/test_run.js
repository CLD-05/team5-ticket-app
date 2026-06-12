import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';

const users = new SharedArray('users', function () {
    return JSON.parse(open('./users.json'));
});

export const options = {
    scenarios: {
        test_scenario: {
            executor: 'constant-vus',
            vus: 5,
            duration: '10s',
        },
    },
};

const BASE_URL = 'http://k8s-ticketin-ticketse-acf7379724-1452042433.ap-northeast-2.elb.amazonaws.com/api/v1';
const TEST_PASSWORD = 'password';

export function setup() {
    console.log('--- [Setup] 사전 로그인 시작 ---');
    const tokens = [];
    const totalTestUsers = Math.min(users.length, 5); // Test with 5 users
    
    for (let i = 0; i < totalTestUsers; i++) {
        const user = users[i];
        const payload = JSON.stringify({ email: user.username, password: user.password || TEST_PASSWORD });
        const loginRes = http.post(`${BASE_URL}/auth/login`, payload, { headers: { 'Content-Type': 'application/json' } });
        if (loginRes.status === 200) {
            tokens.push(loginRes.json().accessToken);
        } else {
            console.log(`[Setup Fail] User: ${user.username}, Status: ${loginRes.status}, Body: ${loginRes.body}`);
        }
    }
    console.log(`--- [Setup] 로그인 성공 토큰 수: ${tokens.length} ---`);
    return { tokens: tokens };
}

export default function (data) {
    const tokens = data.tokens;
    if (!tokens || tokens.length === 0) return;

    const myTokenIndex = (exec.vu.idInInstance - 1) % tokens.length;
    const token = tokens[myTokenIndex];
    const params = {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };

    // 1단계. 대기열 참가
    const joinRes = http.post(`${BASE_URL}/shows/1/queue/join`, null, params);
    check(joinRes, { 'join queue successful': (r) => r.status === 200 });
    sleep(1);

    // 2단계. 대기열 대기 (ALLOW 판정이 날 때까지 2초마다 최대 5번 조회)
    let allowed = false;
    for (let i = 0; i < 5; i++) {
        const statusRes = http.get(`${BASE_URL}/shows/1/queue/status`, params);
        if (statusRes.json().status === 'ACTIVE' || statusRes.json().canEnter === true) {
            allowed = true;
            break;
        }
        sleep(2);
    }

    if (!allowed) {
        console.log(`[Queue Timeout] VU ID ${exec.vu.idInInstance} did not get ALLOWED status.`);
        return;
    }

    // 3단계. 좌석 임시 선택 (락 획득)
    const seatId = (exec.vu.idInInstance % 100) + 1; // 1~100번 좌석 분산
    const holdRes = http.post(`${BASE_URL}/seats/${seatId}/hold`, null, params);
    if (!check(holdRes, { 'hold seat successful': (r) => r.status === 200 })) {
        console.log(`[Hold Fail] Seat: ${seatId}, Status: ${holdRes.status}, Body: ${holdRes.body}`);
        return;
    }

    // 4단계. 예매 승인 요청 (비동기 처리 시작 - SQS 전송)
    const bookingRes = http.post(`${BASE_URL}/bookings?seatId=${seatId}`, null, params);
    check(bookingRes, { 'booking requested successful': (r) => r.status === 202 });
}
