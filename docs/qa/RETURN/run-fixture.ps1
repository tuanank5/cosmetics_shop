param([Parameter(Mandatory)][string]$Checkout, [Parameter(Mandatory)][string]$MavenRepo,
      [Parameter(Mandatory)][string]$PlaywrightCore,
      [string]$EvidenceDirectory,
      [string]$Suite = 'KiemDinhDoiTra_MariaDb_Test,CatalogDonHang_MariaDb_Test,DonHangCOD_MariaDb_Test,PhieuGiamGia_MariaDb_Test,Momo_MariaDb_Test,Audit_MariaDb_Test',
      [switch]$SkipMigration,
      [string]$MariaBin = 'C:/Program Files/MariaDB 12.2/bin')
$ErrorActionPreference = 'Stop'
$build = (Resolve-Path $Checkout).Path
if (!$EvidenceDirectory) { $EvidenceDirectory = $PSScriptRoot }
$evidence = (Resolve-Path $EvidenceDirectory).Path
$repo = (Resolve-Path $MavenRepo).Path
$env:BASE_PLAYWRIGHT = (Resolve-Path $PlaywrightCore).Path
$env:RETURN_BROWSER_SCRIPT = Join-Path $PSScriptRoot 'browser.cjs'
if (!(Test-Path -LiteralPath (Join-Path $build 'pom.xml') -PathType Leaf)) { throw 'Missing checkout pom.xml' }
if (!(Test-Path -LiteralPath (Join-Path $env:BASE_PLAYWRIGHT 'package.json') -PathType Leaf)) { throw 'Missing playwright-core' }
if (!(Test-Path -LiteralPath (Join-Path $MariaBin 'mariadbd.exe') -PathType Leaf)) { throw 'Missing MariaDB binaries' }
if (!$env:JAVA_HOME -or !(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'release') -PathType Leaf) -or
    !(Select-String -LiteralPath (Join-Path $env:JAVA_HOME 'release') -Pattern '^JAVA_VERSION="21[."]' -Quiet)) { throw 'Select JDK 21 in JAVA_HOME' }
$null = Get-Command mvn.cmd,node.exe -ErrorAction Stop
$dir = New-Item -ItemType Directory (Join-Path $build ('target/return-fixture-' + [guid]::NewGuid().ToString('N')))
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
    $actualDir = & "$MariaBin/mariadb.exe" @client --batch --skip-column-names --execute='SELECT @@datadir'
    if ($LASTEXITCODE -or (Get-Item -LiteralPath $actualDir).FullName.TrimEnd('\') -ne $dbDir) {
        throw 'Unexpected fixture data directory'
    }
    $schemas = @('return_fixture','beautyshop_demo_setup_unmanaged','beautyshop_demo_setup_drift')
    $schemas += 1..10 | ForEach-Object { "beautyshop_demo_setup_v$_" }
    foreach ($schema in $schemas) {
        & "$MariaBin/mariadb.exe" @client "--execute=CREATE DATABASE $schema CHARACTER SET utf8mb4"
        if ($LASTEXITCODE) { throw "Fixture schema $schema failed" }
    }
    Remove-Item Env:MYSQL_PWD
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $secret
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$port/return_fixture"
    $env:P01_MARIADB_TEST = 'true'
    $env:APP_ACCOUNT_MAIL_ENABLED = 'false'
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
    $env:SPRING_FLYWAY_ENABLED = 'true'
    $env:MAIL_HOST = '127.0.0.1'
    $env:MAIL_PORT = '1'
    $env:TEMP = (New-Item -ItemType Directory (Join-Path $dir.FullName 'temp')).FullName
    $env:TMP = $env:TEMP
    $env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:1/return_fixture?serverSelectionTimeoutMS=300'
    Set-Location $build
    function Run-Suite($suite, $file) {
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" "-Dtest=$suite" test 2>&1 |
            ForEach-Object { if ("$_" -match 'Tests run: [0-9]+, Failures:|BUILD (SUCCESS|FAILURE)|^\[RETURN WebKit\] PASS|RETURN WebKit failed at|Successfully applied 11 migrations to schema .return_fixture.|Initialized JPA EntityManagerFactory') { "$_" } } |
            Set-Content -Encoding utf8 (Join-Path $evidence $file)
        if ($LASTEXITCODE) { throw "Review fixture results in $file" }
    }
    Run-Suite $Suite 'integration.txt'
    if ($SkipMigration) { return }
    $env:LOCAL_BOOTSTRAP_MIGRATION_FIXTURE = 'true'
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$port/beautyshop_demo_setup"
    Run-Suite 'LocalDatabaseMigration_MariaDb_Test' 'migration.txt'
} finally {
    if (!$server.HasExited) { Stop-Process -Id $server.Id }
}
