$ErrorActionPreference = 'Stop'
$bin = 'C:/Program Files/MariaDB 12.2/bin'
$port = 33373
$probe = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
$probe.Start()
$probe.Stop()

$root = (Resolve-Path .).Path
$qa = Join-Path $root 'docs/qa/AUTH-02'
$data = Join-Path $root ('target/auth-02-db-' + [Guid]::NewGuid().ToString('N'))
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
    $env:AUTH_02_MIGRATION_FIXTURE = 'true'
    $env:P01_MARIADB_TEST = 'true'
    $env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:1/auth_02?serverSelectionTimeoutMS=300&connectTimeoutMS=300'
    $ErrorActionPreference = 'Continue'
    & mvn -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" '-Dtest=XacThucDaPhuongThuc_MariaDb_Test,XacThuc_MariaDb_Test,DonHangCOD_MariaDb_Test,Momo_MariaDb_Test,XacThucMigration_MariaDb_Test,OAuth2Callback_MariaDb_Test' test *> (Join-Path $qa 'mvn-mariadb.log')
    $result = $LASTEXITCODE
    Get-Content (Join-Path $qa 'mvn-mariadb.log') -Tail 32
    exit $result
} finally {
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
