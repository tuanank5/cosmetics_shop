param([switch]$Baseline)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot'
$bin = 'C:/Program Files/MariaDB 12.2/bin'
$port = 33377
$probe = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
$probe.Start()
$probe.Stop()

$root = (Resolve-Path .).Path
$qa = Join-Path $root 'docs/qa/NAV-01'
$data = Join-Path $root ('target/nav-01-db-' + [Guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $data
$secret = [Guid]::NewGuid().ToString('N')
& "$bin/mariadb-install-db.exe" "--datadir=$data" "--password=$secret" "--port=$port" --silent | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'MariaDB init failed' }

$db = Start-Process -FilePath "$bin/mariadbd.exe" -WindowStyle Hidden -PassThru -ArgumentList @(
    ('--defaults-file="' + (Join-Path $data 'my.ini') + '"'),
    '--bind-address=127.0.0.1', '--console'
) -RedirectStandardOutput (Join-Path $qa 'mariadb-stdout.log') -RedirectStandardError (Join-Path $qa 'mariadb-stderr.log')
try {
    $env:MYSQL_PWD = $secret
    $clientArgs = @('--skip-ssl','--protocol=tcp','--host=127.0.0.1',"--port=$port",'--user=root')
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        $ready = [bool] (Test-NetConnection -ComputerName 127.0.0.1 -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue)
        if ($ready) { break }
        Start-Sleep -Milliseconds 300
    }
    if (!$ready) { throw 'MariaDB isolated port did not open' }
    & "$bin/mariadb.exe" @clientArgs --execute='CREATE DATABASE auth_02_test CHARACTER SET utf8mb4; CREATE DATABASE auth_02_v5 CHARACTER SET utf8mb4; CREATE DATABASE auth_02_v6 CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE -ne 0) { throw 'Could not create isolated schema' }
    Remove-Item Env:MYSQL_PWD
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$port/auth_02_test"
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $secret
    $env:P01_MARIADB_TEST = 'true'
    $env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:1/beautyshop_test_mongo'
    $env:MONGODB_URI = $env:MONGODB_TEST_URI
    $env:APP_UPLOADS_SKU_DIR = Join-Path $data 'uploads'
    $mode = if ($Baseline) { 'before' } else { 'after' }
    $log = Join-Path $qa "fixture-$mode.log"
    $ErrorActionPreference = 'Continue'
    $tests = if ($Baseline) { 'HttpLoi_MariaDb_Test' } else {
        'HttpLoi_MariaDb_Test,XacThuc_MariaDb_Test,XacThucDaPhuongThuc_MariaDb_Test,OAuth2Callback_MariaDb_Test,DonHangCOD_MariaDb_Test,LoginRedirectTest,PhieuGiamGia_MariaDb_Test'
    }
    & mvn -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=$tests" test *> $log
    $code = $LASTEXITCODE
    $safe = @([IO.File]::ReadAllLines($log) | Where-Object { $_ -notmatch '(?i)password|token=|cookie:|authorization:' })
    [IO.File]::WriteAllLines($log, $safe)
    Select-String $log -Pattern 'Tests run:|BUILD SUCCESS|BUILD FAILURE'
    if ($code) { exit $code }
    $jar = Join-Path $root $(if ($Baseline) { 'target/nav-01-before.jar' } else { 'target/beautyshop-0.0.1-SNAPSHOT.jar' })
    $webPort = 18092
    $probeWeb = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $webPort)
    $probeWeb.Start()
    $probeWeb.Stop()
    $site = "http://127.0.0.1:$webPort"
    $argsForJava = @('-Xmx512m', '-jar', ('"' + $jar + '"'), "--server.port=$webPort", '--server.address=127.0.0.1')
    $web = Start-Process -FilePath "$env:JAVA_HOME/bin/java.exe" -WindowStyle Hidden -PassThru -ArgumentList $argsForJava -RedirectStandardOutput (Join-Path $data 'web.log') -RedirectStandardError (Join-Path $data 'web-err.log')
    $ready = $false
    for ($i = 0; $i -lt 35 -and !$web.HasExited; $i++) {
        try {
            $r = Invoke-WebRequest -UseBasicParsing $site -TimeoutSec 5 -ErrorAction Stop
            $ready = $r.StatusCode -eq 200
            if ($ready) { break }
        } catch { Start-Sleep -Seconds 1 }
    }
    if (!$ready) { throw 'NAV fixture web did not start' }
    & node (Join-Path $qa 'browser-check.mjs') $site $data $mode
    exit $LASTEXITCODE
} finally {
    if ($web -and !$web.HasExited) { Stop-Process -Id $web.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
