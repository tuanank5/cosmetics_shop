param([switch]$Ui)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot'
$bin = 'C:/Program Files/MariaDB 12.2/bin'
$port = 33374
$probe = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
$probe.Start()
$probe.Stop()

$root = (Resolve-Path .).Path
$qa = Join-Path $root 'docs/qa/SEC-01'
if ($Ui -and (Test-Path -LiteralPath (Join-Path $qa 'ui-finished.signal'))) {
    Remove-Item -LiteralPath (Join-Path $qa 'ui-finished.signal')
}
$data = Join-Path $root ('target/sec-01-db-' + [Guid]::NewGuid().ToString('N'))
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
    & "$bin/mariadb.exe" @clientArgs --execute='CREATE DATABASE sec_01_test CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE -ne 0) { throw 'Could not create isolated schema' }
    Remove-Item Env:MYSQL_PWD
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$port/sec_01_test"
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $secret
    $env:P01_MARIADB_TEST = 'true'
    $env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:1/sec_01?serverSelectionTimeoutMS=300&connectTimeoutMS=300'
    $env:APP_UPLOADS_SKU_DIR = Join-Path $data 'uploads'
    $ErrorActionPreference = 'Continue'
    $tests = if ($Ui) { 'HttpLoi_MariaDb_Test' } else { 'HttpLoi_MariaDb_Test,DonHangCOD_MariaDb_Test,Momo_MariaDb_Test,XacThuc_MariaDb_Test,XacThucDaPhuongThuc_MariaDb_Test' }
    $uiFlag = if ($Ui) { '-Dsec01.ui=true' } else { '-Dsec01.ui=false' }
    & mvn -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=$tests" $uiFlag test *> (Join-Path $qa 'mvn-mariadb.log')
    $result = $LASTEXITCODE
    # Keep local evidence free of credential/cookie/token lines, including on test failure.
    $log = Join-Path $qa 'mvn-mariadb.log'
    $safe = @([IO.File]::ReadAllLines($log) | Where-Object { $_ -notmatch '(?i)generated security password|password=|token=|authorization:|cookie:' })
    [IO.File]::WriteAllLines($log, $safe, [Text.UTF8Encoding]::new($false))
    Get-Content (Join-Path $qa 'mvn-mariadb.log') -Tail 32
    exit $result
} finally {
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
