param([Parameter(Mandatory)][string]$Checkout, [Parameter(Mandatory)][string]$MavenRepo,
      [Parameter(Mandatory)][string]$PlaywrightCore,
      [string]$EvidenceDirectory = $PSScriptRoot,
      [string]$Suite = 'ThuTaiKhoan_MariaDb_Test,XacThuc_MariaDb_Test,OAuth2Callback_MariaDb_Test,Audit_MariaDb_Test,XacThucDaPhuongThuc_MariaDb_Test',
      [switch]$SkipMigration,
      [switch]$VerifyAccountSql,
      [string]$MariaBin = 'C:/Program Files/MariaDB 12.2/bin')
$ErrorActionPreference = 'Stop'
$build = (Resolve-Path $Checkout).Path
$evidence = (Resolve-Path $EvidenceDirectory).Path
$repo = (Resolve-Path $MavenRepo).Path
$env:BASE_PLAYWRIGHT = (Resolve-Path $PlaywrightCore).Path
$env:ACCOUNT_BROWSER_SCRIPT = Join-Path $PSScriptRoot 'browser.cjs'
$env:AUTH_BROWSER_SCRIPT = (Resolve-Path (Join-Path $PSScriptRoot '../AUTH/browser.cjs')).Path
$dir = New-Item -ItemType Directory (Join-Path $build ('target/account-fixture-' + [guid]::NewGuid().ToString('N')))
$dbDir = Join-Path $dir.FullName 'db'
$probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$probe.Start()
$port = $probe.LocalEndpoint.Port
$probe.Stop()
$secret = [guid]::NewGuid().ToString('N')
& "$MariaBin/mariadb-install-db.exe" "--datadir=$dbDir" "--password=$secret" "--port=$port" --silent | Out-Null
if ($LASTEXITCODE) { throw 'MariaDB fixture init failed' }
$server = Start-Process "$MariaBin/mariadbd.exe" -WindowStyle Hidden -PassThru -ArgumentList @(
    ('--defaults-file="' + (Join-Path $dbDir 'my.ini') + '"'), '--bind-address=127.0.0.1', '--console'
) -RedirectStandardOutput (Join-Path $dir.FullName 'server.log') -RedirectStandardError (Join-Path $dir.FullName 'server.err')
try {
    $env:MYSQL_PWD = $secret
    $client = @('--skip-ssl', '--protocol=tcp', '--host=127.0.0.1', "--port=$port", '--user=root')
    $ready = $false
    for ($i=0; $i -lt 80; $i++) {
        if ($server.HasExited) { throw 'MariaDB fixture exited' }
        try {
            & "$MariaBin/mariadb.exe" @client --execute='SELECT 1' 2>$null | Out-Null
            if (!$LASTEXITCODE) { $ready = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 250
    }
    if (!$ready) { throw 'Isolated MariaDB did not become ready' }
    $schemas = @('account_app','account_outbox_fixture','beautyshop_demo_setup_unmanaged','beautyshop_demo_setup_drift')
    $schemas += 1..9 | ForEach-Object { "beautyshop_demo_setup_v$_" }
    foreach ($schema in $schemas) {
        & "$MariaBin/mariadb.exe" @client "--execute=CREATE DATABASE $schema CHARACTER SET utf8mb4"
        if ($LASTEXITCODE) { throw "Fixture schema $schema failed" }
    }
    Remove-Item Env:MYSQL_PWD
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $secret
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$port/account_app"
    $env:ACCOUNT_OUTBOX_DB_URL = "jdbc:mariadb://127.0.0.1:$port/account_outbox_fixture"
    $env:P01_MARIADB_TEST = 'true'
    $env:APP_ACCOUNT_MAIL_ENABLED = 'false'
    $env:MAIL_HOST = '127.0.0.1'
    $env:MAIL_PORT = '1'
    $env:TEMP = (New-Item -ItemType Directory (Join-Path $dir.FullName 'temp')).FullName
    $env:TMP = $env:TEMP
    $env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:1/account_fixture?serverSelectionTimeoutMS=300'
    Set-Location $build
    function Run-Suite($suite, $file) {
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" "-Dtest=$suite" test 2>&1 |
            ForEach-Object { if ("$_" -match 'Tests run: [0-9]+, Failures:|BUILD (SUCCESS|FAILURE)|\[(ACCOUNT|AUTH) WebKit\]|ACCOUNT WebKit failed at') { "$_" } } |
            Set-Content -Encoding utf8 (Join-Path $evidence $file)
        if ($LASTEXITCODE) { throw "Review fixture results in $file" }
    }
    Run-Suite $Suite 'integration.txt'
    if ($VerifyAccountSql) {
        $env:MYSQL_PWD = $secret
        try {
            Get-Content -Raw -Encoding utf8 (Join-Path $PSScriptRoot 'final-review/check.sql') |
                & "$MariaBin/mariadb.exe" @client --batch account_outbox_fixture |
                Set-Content -Encoding utf8 (Join-Path $evidence 'sql.txt')
            if ($LASTEXITCODE) { throw 'Fixture SQL verification failed' }
        } finally { Remove-Item Env:MYSQL_PWD }
    }
    if ($SkipMigration) { return }
    $env:LOCAL_BOOTSTRAP_MIGRATION_FIXTURE = 'true'
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$port/beautyshop_demo_setup"
    Run-Suite 'LocalDatabaseMigration_MariaDb_Test' 'migration.txt'
} finally {
    if (!$server.HasExited) { Stop-Process -Id $server.Id }
}
