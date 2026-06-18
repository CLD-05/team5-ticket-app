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
