/**
 * [사전 토큰 발급 스크립트]
 *
 * k6 부하 테스트 실행 전에 이 스크립트를 먼저 실행하세요.
 * 실제 티켓팅처럼 유저들은 오픈 전 미리 로그인된 상태를 시뮬레이션합니다.
 *
 * 사용법:
 *   node generate_tokens.js --url http://your-server/api/v1 --count 1000
 *
 * 옵션:
 *   --url      API 서버 주소 (기본값: http://localhost:8080/api/v1)
 *   --count    발급할 유저 수 (기본값: 1000, 최대 VU 수 이상으로 설정)
 *   --password 유저 비밀번호 (기본값: password)
 *   --batch    배치당 동시 요청 수 (기본값: 50, 서버 부하에 따라 조절)
 *   --delay    배치 간 대기 시간(ms) (기본값: 300)
 *   --output   저장할 파일 경로 (기본값: ./tokens.json)
 */

const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const urlModule = require('url');

// ── 인자 파싱 ──────────────────────────────────────────────
const args = process.argv.slice(2);
const getArg = (name, defaultVal) => {
    const idx = args.indexOf(name);
    return idx !== -1 && args[idx + 1] ? args[idx + 1] : defaultVal;
};

const BASE_URL  = getArg('--url',      'http://localhost:8080/api/v1');
const COUNT     = parseInt(getArg('--count',   '1000'));
const PASSWORD  = getArg('--password', 'password');
const BATCH     = parseInt(getArg('--batch',   '50'));
const DELAY_MS  = parseInt(getArg('--delay',   '300'));
const OUTPUT    = getArg('--output',   path.join(__dirname, 'tokens.json'));

// ── 로그인 요청 (Promise) ──────────────────────────────────
function login(email) {
    return new Promise((resolve) => {
        const loginUrl = `${BASE_URL}/auth/login`;
        const parsed   = urlModule.parse(loginUrl);
        const body     = JSON.stringify({ email, password: PASSWORD });

        const options = {
            hostname: parsed.hostname,
            port:     parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
            path:     parsed.path,
            method:   'POST',
            headers: {
                'Content-Type':   'application/json',
                'Content-Length': Buffer.byteLength(body),
            },
        };

        const client = parsed.protocol === 'https:' ? https : http;
        const req = client.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                if (res.statusCode === 200) {
                    try {
                        const json = JSON.parse(data);
                        resolve(json.accessToken || json.token || null);
                    } catch {
                        resolve(null);
                    }
                } else {
                    resolve(null);
                }
            });
        });

        req.on('error', () => resolve(null));
        req.setTimeout(8000, () => { req.destroy(); resolve(null); });
        req.write(body);
        req.end();
    });
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// ── 메인 ──────────────────────────────────────────────────
async function main() {
    console.log('');
    console.log('╔══════════════════════════════════════════╗');
    console.log('║     k6 사전 토큰 발급 스크립트           ║');
    console.log('╚══════════════════════════════════════════╝');
    console.log(`  대상 서버 : ${BASE_URL}`);
    console.log(`  유저 수   : ${COUNT.toLocaleString()}명`);
    console.log(`  배치 크기 : ${BATCH}명 / 딜레이: ${DELAY_MS}ms`);
    console.log(`  저장 경로 : ${OUTPUT}`);
    console.log('');

    const tokens = [];
    let success  = 0;
    let fail     = 0;
    const totalBatches = Math.ceil(COUNT / BATCH);
    const startTime = Date.now();

    for (let b = 0; b < totalBatches; b++) {
        const start = b * BATCH + 1;
        const end   = Math.min((b + 1) * BATCH, COUNT);

        const promises = [];
        for (let i = start; i <= end; i++) {
            // 로그인 필드: email (user1@example.com 형식)
            promises.push(login(`user${i}@example.com`));
        }


        const results = await Promise.all(promises);
        results.forEach(token => {
            if (token) { tokens.push(token); success++; }
            else        { fail++; }
        });

        const progress = Math.round((end / COUNT) * 100);
        const bar = '█'.repeat(Math.floor(progress / 5)) + '░'.repeat(20 - Math.floor(progress / 5));
        process.stdout.write(`\r  [${bar}] ${progress}% | 성공: ${success} / 실패: ${fail} `);

        if (b < totalBatches - 1) {
            await sleep(DELAY_MS);
        }
    }

    const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
    console.log(`\n`);
    console.log('──────────────────────────────────────────');

    if (tokens.length === 0) {
        console.error('❌ 토큰 발급에 실패했습니다. 서버 연결 상태를 확인하세요.');
        process.exit(1);
    }

    fs.writeFileSync(OUTPUT, JSON.stringify(tokens, null, 2), 'utf8');

    console.log(`✅ 완료! (${elapsed}초 소요)`);
    console.log(`   성공 : ${success}개`);
    console.log(`   실패 : ${fail}개`);
    console.log(`   저장 : ${OUTPUT}`);

    if (fail > 0) {
        const rate = ((fail / COUNT) * 100).toFixed(1);
        console.log('');
        console.log(`⚠️  실패율 ${rate}% — --batch 값을 줄이거나 --delay를 늘려 재시도하세요.`);
        console.log(`   예) node generate_tokens.js --batch 20 --delay 500 ...`);
    }

    console.log('');
    console.log('→ 이제 k6 부하 테스트를 실행하세요.');
    console.log(`  k6 run advanced_spike_test.js --env TARGET_URL=${BASE_URL}`);
    console.log('');
}

main().catch(err => {
    console.error('\n❌ 오류:', err.message);
    process.exit(1);
});
