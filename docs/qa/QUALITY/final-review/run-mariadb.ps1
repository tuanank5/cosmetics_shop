param([Parameter(Mandatory)][string]$Checkout, [Parameter(Mandatory)][string]$MavenRepo,
      [string]$EvidenceDirectory = $PSScriptRoot)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path $MavenRepo).Path
$evidence = (Resolve-Path $EvidenceDirectory).Path
Set-Location (Resolve-Path $Checkout)
$bin = 'C:/Program Files/MariaDB 12.2/bin'
$port = 33472
$probe = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
$probe.Start()
$probe.Stop()

$root = (Resolve-Path .).Path
$qa = (Resolve-Path docs/qa/AUTH).Path
$data = Join-Path $root ('target/auth-db-' + [Guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $data
$secret = [Guid]::NewGuid().ToString('N')
& "$bin/mariadb-install-db.exe" "--datadir=$data" "--password=$secret" "--port=$port" --silent | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'MariaDB init failed' }

$db = Start-Process -FilePath "$bin/mariadbd.exe" -WindowStyle Hidden -PassThru -ArgumentList @(
    ('--defaults-file="' + (Join-Path $data 'my.ini') + '"'),
    '--bind-address=127.0.0.1', '--console'
) -RedirectStandardOutput (Join-Path $data 'server-stdout.log') -RedirectStandardError (Join-Path $data 'server-stderr.log')
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
    $env:AUTH_02_MIGRATION_FIXTURE = 'true'
    $env:P01_MARIADB_TEST = 'true'
    $env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:1/auth_02?serverSelectionTimeoutMS=300&connectTimeoutMS=300'
    $ErrorActionPreference = 'Continue'
    $env:AUTH_BROWSER_SCRIPT = Join-Path $qa 'browser.cjs'
    $env:TEMP = (New-Item -ItemType Directory (Join-Path $data 'temp')).FullName
    $env:TMP = $env:TEMP
    & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" '-Dtest=CheckoutIntegrityTest,AdminOrderList_MariaDb_Test,PhieuGiamGia_MariaDb_Test,BienTheSanPham_MariaDb_Test,CatalogDonHang_MariaDb_Test,TuVanFaq_MariaDb_Test,TuVanRag_MariaDb_Test,MatKhauMoiTest,XacThucDaPhuongThuc_MariaDb_Test,XacThuc_MariaDb_Test,DonHangCOD_MariaDb_Test,Momo_MariaDb_Test,XacThucMigration_MariaDb_Test,OAuth2Callback_MariaDb_Test' test 2>&1 |
        ForEach-Object { if ("$_" -match 'Tests run: \d+, Failures:|BUILD (SUCCESS|FAILURE)|\[AUTH WebKit\]|Browser check failed at') { "$_" } } |
        Set-Content -Encoding utf8 (Join-Path $evidence 'integration.txt')
    $result = $LASTEXITCODE
    exit $result
} finally {
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
