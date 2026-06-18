const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const PORT = 3000;

const server = http.createServer((req, res) => {
    const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
    
    // Serve HTML Dashboard
    if (req.method === 'GET' && (req.url === '/' || req.url === '/index.html' || req.url === '/k6_studio.html')) {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        fs.createReadStream(path.join(__dirname, 'k6_studio.html')).pipe(res);
        return;
    }
    
    // Run Test API (Streams chunked console output in real-time)
    if (req.method === 'POST' && parsedUrl.pathname === '/api/run') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const scriptContent = data.script;
                const targetUrl = data.targetUrl;
                const usePrometheus = data.usePrometheus;
                
                // Write temp script file
                const tempScriptPath = path.join(__dirname, 'temp_run_script.js');
                fs.writeFileSync(tempScriptPath, scriptContent);
                
                res.writeHead(200, {
                    'Content-Type': 'text/plain; charset=utf-8',
                    'Transfer-Encoding': 'chunked',
                    'Access-Control-Allow-Origin': '*'
                });
                
                res.write(`[SYSTEM] k6 프로세스를 시작합니다...\n`);
                res.write(`[SYSTEM] Target API URL: ${targetUrl}\n`);
                
                const args = ['run'];
                if (usePrometheus) {
                    args.push('--out', 'experimental-prometheus-rw');
                    res.write(`[SYSTEM] 프로메테우스 연동 활성화 (Remote Write)\n`);
                }
                args.push(tempScriptPath);
                
                // Spawn k6 process
                const k6Process = spawn('k6', args, {
                    cwd: __dirname,
                    env: {
                        ...process.env,
                        TARGET_URL: targetUrl
                    }
                });
                
                k6Process.stdout.on('data', (chunk) => {
                    res.write(chunk);
                });
                
                k6Process.stderr.on('data', (chunk) => {
                    res.write(chunk);
                });
                
                k6Process.on('error', (err) => {
                    res.write(`\n[SYSTEM ERROR] k6 프로세스 생성 실패. 시스템에 k6가 설치되어 있고 PATH에 등록되어 있는지 확인하십시오. (${err.message})\n`);
                    res.end();
                });
                
                k6Process.on('close', (code) => {
                    res.write(`\n[SYSTEM] k6 테스트가 종료되었습니다. (종료 코드: ${code})\n`);
                    res.end();
                    
                    // Cleanup temp file
                    try {
                        if (fs.existsSync(tempScriptPath)) {
                            fs.unlinkSync(tempScriptPath);
                        }
                    } catch (e) {}
                });
                
                // If client connection is aborted/closed, kill k6 process immediately
                res.on('close', () => {
                    k6Process.kill();
                    try {
                        if (fs.existsSync(tempScriptPath)) {
                            fs.unlinkSync(tempScriptPath);
                        }
                    } catch (e) {}
                });
                
            } catch (err) {
                res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
                res.end(`서버 내부 에러: ${err.message}`);
            }
        });
        return;
    }

    // Generate Tokens API (streams output in real-time)
    if (req.method === 'POST' && parsedUrl.pathname === '/api/generate-tokens') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const targetUrl = data.url || 'http://localhost:8080/api/v1';
                const count     = parseInt(data.count || '1000');

                res.writeHead(200, {
                    'Content-Type': 'text/plain; charset=utf-8',
                    'Transfer-Encoding': 'chunked',
                    'Access-Control-Allow-Origin': '*'
                });

                res.write(`[SYSTEM] 토큰 발급 시작: ${count}명 / 대상: ${targetUrl}\n`);

                const tokenProcess = spawn('node', [
                    path.join(__dirname, 'generate_tokens.js'),
                    '--url',   targetUrl,
                    '--count', String(count),
                    '--output', path.join(__dirname, 'tokens.json')
                ], { cwd: __dirname });

                tokenProcess.stdout.on('data', chunk => res.write(chunk));
                tokenProcess.stderr.on('data', chunk => res.write(chunk));
                tokenProcess.on('error', err => {
                    res.write(`\n[ERROR] ${err.message}\n`);
                    res.end();
                });
                tokenProcess.on('close', code => {
                    const ok = code === 0;
                    res.write(`\n[SYSTEM] ${ok ? '✅ 토큰 발급 완료! 이제 부하 테스트를 실행하세요.' : '❌ 발급 실패 (종료 코드: ' + code + ')'}\n`);
                    res.end();
                });
                res.on('close', () => tokenProcess.kill());

            } catch (err) {
                res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
                res.end(`서버 내부 에러: ${err.message}`);
            }
        });
        return;
    }

    // Reset Database API
    if (req.method === 'POST' && parsedUrl.pathname === '/api/reset-database') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const targetUrl = data.url || '';
                
                res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
                
                if (targetUrl.includes('localhost') || targetUrl.includes('127.0.0.1')) {
                    res.write(`[SYSTEM] 로컬 도커 DB 초기화 시작...\n`);
                    
                    // Run reset on local docker container
                    const mysqlCmd = `docker exec -i ticketing-mysql mysql -uroot -ppassword ticketing -e "DELETE FROM bookings; UPDATE seats SET status = 'AVAILABLE' WHERE status <> 'AVAILABLE';"`;
                    const redisCmd = `docker exec -i ticketing-redis redis-cli FLUSHALL`;
                    
                    const { exec } = require('child_process');
                    exec(mysqlCmd, (err, stdout, stderr) => {
                        if (err) {
                            res.write(`[ERROR] MySQL 초기화 실패: ${stderr || err.message}\n`);
                            res.end();
                            return;
                        }
                        res.write(`[SUCCESS] MySQL 초기화 완료 (예매 내역 삭제, 좌석 AVAILABLE 복구)\n`);
                        
                        exec(redisCmd, (rErr, rStdout, rStderr) => {
                            if (rErr) {
                                res.write(`[WARNING] Redis 초기화 실패: ${rStderr || rErr.message}\n`);
                            } else {
                                res.write(`[SUCCESS] Redis 대기열 및 락 초기화 완료 (FLUSHALL)\n`);
                            }
                            res.write(`\n[SYSTEM] ✅ 로컬 DB 및 캐시 초기화 완료!\n`);
                            res.end();
                        });
                    });
                } else if (targetUrl.includes('amazonaws.com')) {
                    res.write(`[SYSTEM] AWS EKS 개발 DB 초기화 시작...\n`);
                    
                    const mysql = require('mysql2');
                    const connection = mysql.createConnection({
                        host: '127.0.0.1',
                        port: 3307,
                        user: 'ticketadmin',
                        password: '[YiD)nfU&UuuIplHVt>TieAD',
                        database: 'ticketing',
                        multipleStatements: true
                    });
                    
                    connection.connect(err => {
                        if (err) {
                            res.write(`[ERROR] AWS DB 연결 실패 (3307 터널 확인 필요): ${err.message}\n`);
                            res.end();
                            return;
                        }
                        
                        const queries = "DELETE FROM bookings; UPDATE seats SET status = 'AVAILABLE' WHERE status <> 'AVAILABLE';";
                        connection.query(queries, (qErr, results) => {
                            connection.end();
                            if (qErr) {
                                res.write(`[ERROR] AWS DB 초기화 SQL 실행 실패: ${qErr.message}\n`);
                            } else {
                                res.write(`[SUCCESS] AWS DB 초기화 완료 (예매 내역 삭제, 좌석 AVAILABLE 복구)\n`);
                                res.write(`\n[SYSTEM] ✅ AWS DB 초기화 완료!\n`);
                            }
                            res.end();
                        });
                    });
                } else {
                    res.write(`[ERROR] 지원하지 않는 대상 주소 형식입니다. (${targetUrl})\n`);
                    res.end();
                }
            } catch (err) {
                res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
                res.end(`서버 에러: ${err.message}`);
            }
        });
        return;
    }

    // Token status check
    if (req.method === 'GET' && parsedUrl.pathname === '/api/tokens-status') {
        const tokensPath = path.join(__dirname, 'tokens.json');
        res.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
        if (fs.existsSync(tokensPath)) {
            try {
                const tokens = JSON.parse(fs.readFileSync(tokensPath, 'utf8'));
                res.end(JSON.stringify({ exists: true, count: tokens.length }));
            } catch {
                res.end(JSON.stringify({ exists: true, count: 0, error: 'parse error' }));
            }
        } else {
            res.end(JSON.stringify({ exists: false, count: 0 }));
        }
        return;
    }

    
    // Serve static files
    if (req.method === 'GET' && req.url === '/users.json') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        fs.createReadStream(path.join(__dirname, 'users.json')).pipe(res);
        return;
    }
    
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
});

server.listen(PORT, () => {
    console.log(`K6 Studio local server running at: http://localhost:${PORT}`);
});
