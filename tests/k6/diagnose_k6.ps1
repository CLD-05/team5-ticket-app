# K6 테스트 환경 자가 진단 스크립트 (Windows PowerShell용)
# 사용법: 
#   1. 이 파일을 복사하여 `team5-ticket-app/tests/k6/` 폴더 아래에 `diagnose_k6.ps1`로 저장합니다.
#   2. PowerShell을 열고 해당 폴더로 이동한 뒤 `.\diagnose_k6.ps1`을 실행합니다.
#   3. 출력되는 결과를 복사하여 팀원에게 전달하면 빠르게 문제 원인을 진단할 수 있습니다.

$ErrorActionPreference = "SilentlyContinue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Clear-Host

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "         5팀 K6 부하 테스트 환경 자가 진단         " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "실행 시간: $(Get-Date)"
Write-Host "현재 경로: $(Get-Location)"
Write-Host ""

$devAlbUrl = "http://k8s-ticketin-ticketse-acf7379724-1452042433.ap-northeast-2.elb.amazonaws.com/api/v1"
$localServerUrl = "http://localhost:3000"

# 1. 파일 존재 여부 검사
Write-Host "[1/6] 필수 파일 체크..." -ForegroundColor Yellow
$serverJsExists = Test-Path "server.js"
$generateJsExists = Test-Path "generate_tokens.js"

if ($serverJsExists -and $generateJsExists) {
    Write-Host "  ✔ [성공] 필수 파일(server.js, generate_tokens.js)이 경로에 존재합니다." -ForegroundColor Green
} else {
    Write-Host "  ❌ [에러] 파일이 누락되었습니다! 반드시 'team5-ticket-app/tests/k6/' 폴더에서 실행해 주세요." -ForegroundColor Red
    if (-not $serverJsExists) { Write-Host "    - server.js 가 없음" -ForegroundColor Red }
    if (-not $generateJsExists) { Write-Host "    - generate_tokens.js 가 없음" -ForegroundColor Red }
}
Write-Host ""

# 2. Node.js 설치 여부 검사
Write-Host "[2/6] Node.js 설치 상태 체크..." -ForegroundColor Yellow
$nodeVersion = node -v 2>$null
if ($nodeVersion) {
    Write-Host "  ✔ [성공] Node.js가 설치되어 있습니다. (버전: $nodeVersion)" -ForegroundColor Green
} else {
    Write-Host "  ❌ [에러] Node.js가 설치되어 있지 않거나 PATH에 등록되지 않았습니다." -ForegroundColor Red
    Write-Host "    - 해결법: https://nodejs.org 에서 LTS 버전을 설치해 주세요." -ForegroundColor Gray
}
Write-Host ""

# 3. server.js 최신 버전(Git pull 및 수정본) 여부 검사
Write-Host "[3/6] server.js 파일 버전(최신 소스 코드) 체크..." -ForegroundColor Yellow
if ($serverJsExists) {
    $content = Get-Content "server.js" -Raw
    $hasTokenRoute = $content -match "generate-tokens"
    $hasResetRoute = $content -match "reset-database"

    if ($hasTokenRoute -and $hasResetRoute) {
        Write-Host "  ✔ [성공] server.js가 최신 버전(토큰 발급 및 초기화 API 포함)입니다." -ForegroundColor Green
    } else {
        Write-Host "  ❌ [에러] server.js가 옛날 버전입니다! 최신 Git 코드가 반영되지 않았습니다." -ForegroundColor Red
        Write-Host "    - 해결법: 'git pull'을 실행하여 최신 코드를 내려받으세요." -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠ [건너뜀] server.js 파일이 없어 검사할 수 없습니다." -ForegroundColor DarkYellow
}
Write-Host ""

# 4. 로컬 Node.js 서버(포트 3000) 구동 상태 검사
Write-Host "[4/6] 로컬 Node.js 대시보드 서버(Port 3000) 구동 상태 체크..." -ForegroundColor Yellow
$localCheck = Invoke-WebRequest -Uri "$localServerUrl" -TimeoutSec 2 2>$null
if ($localCheck.StatusCode -eq 200) {
    Write-Host "  ✔ [성공] 로컬 대시보드 서버가 정상 실행 중입니다." -ForegroundColor Green
} else {
    Write-Host "  ❌ [에러] 로컬 대시보드 서버(Port 3000)가 꺼져 있습니다." -ForegroundColor Red
    Write-Host "    - 해결법: 터미널에서 'node server.js' 명령을 실행하여 서버를 켜주세요." -ForegroundColor Gray
}
Write-Host ""

# 5. AWS EKS 개발 환경 네트워크 연결 체크
Write-Host "[5/6] AWS EKS 개발 환경 로드밸런서 네트워크 접속 체크..." -ForegroundColor Yellow
$dnsResolve = $true
try {
    $ips = [System.Net.Dns]::GetHostAddresses("k8s-ticketin-ticketse-acf7379724-1452042433.ap-northeast-2.elb.amazonaws.com")
    Write-Host "  ✔ DNS 해석 성공 (IP 목록: $($ips -join ', '))" -ForegroundColor Green
} catch {
    $dnsResolve = $false
    Write-Host "  ❌ [에러] EKS 로드밸런서 주소의 DNS 해석에 실패했습니다. (인터넷 연결을 확인하세요)" -ForegroundColor Red
}

if ($dnsResolve) {
    # 80 포트 TCP 커넥션 테스트 (TcpClient 사용으로 속도 및 정확도 향상)
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $connect = $tcpClient.BeginConnect("k8s-ticketin-ticketse-acf7379724-1452042433.ap-northeast-2.elb.amazonaws.com", 80, $null, $null)
    $wait = $connect.AsyncWaitHandle.WaitOne(2000, $false)
    if ($wait -and $tcpClient.Connected) {
        $tcpClient.EndConnect($connect)
        Write-Host "  ✔ [성공] EKS 로드밸런서(80 포트)와 TCP 연결이 성공했습니다." -ForegroundColor Green
    } else {
        Write-Host "  ❌ [에러] EKS 로드밸런서와 TCP 연결을 맺을 수 없습니다. (방화벽 또는 인프라 상태 확인 필요)" -ForegroundColor Red
    }
    $tcpClient.Close()
}
Write-Host ""

# 6. EKS API(인증/로그인) 매핑 상태 검사
Write-Host "[6/6] EKS 로그인 API 동작 상태 체크..." -ForegroundColor Yellow
if ($dnsResolve) {
    $loginUrl = "$devAlbUrl/auth/login"
    $dummyBody = @{ email = "non_existent_user@example.com"; password = "wrong_password" } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $dummyBody -ContentType "application/json" -TimeoutSec 5 2>&1
    } catch {
        $ex = $_
        $response = $ex.Exception.Response
    }

    if ($response -and $response.StatusCode -eq 401) {
        # 잘못된 비밀번호를 보냈을 때 401 Unauthorized가 오면 API 매핑이 완벽하게 동작 중임을 뜻함
        Write-Host "  ✔ [성공] EKS 로그인 API가 정상 동작합니다. (서버에서 401 Unauthorized 확인됨)" -ForegroundColor Green
    } elseif ($response -and $response.StatusCode -eq 404) {
        Write-Host "  ❌ [에러] EKS 로그인 API 접속 시 404 Not Found가 발생했습니다." -ForegroundColor Red
        Write-Host "    - 해결법: EKS 클러스터 내 백엔드 컨테이너가 정상적으로 실행 중인지 배포 상태를 확인해야 합니다." -ForegroundColor Gray
    } else {
        # 예외 상세 정보 파싱
        $statusCode = "알수없음"
        if ($response -and $response.StatusCode) { $statusCode = $response.StatusCode }
        Write-Host "  ❌ [에러] API 호출 실패 (HTTP 상태 코드: $statusCode)" -ForegroundColor Red
        Write-Host "    - 상세 에러: $response" -ForegroundColor DarkRed
    }
} else {
    Write-Host "  ⚠ [건너뜀] DNS 문제로 API 동작 여부를 체크하지 못했습니다." -ForegroundColor DarkYellow
}

Write-Host "`n====================================================" -ForegroundColor Cyan
Write-Host "               자가 진단 프로세스 종료               " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
