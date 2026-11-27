param(
    [ValidateSet('before','after','integration','migration','v5-drift')][string]$Phase,
    [Parameter(Mandatory)][string]$Build,
    [string]$MariaBin,
    [string]$MongoExe,
    [string]$ChromeExe,
    [string]$WebSocketModule,
    [string]$Tests,
    [ValidatePattern('^[a-zA-Z0-9-]+$')][string]$EvidenceTag,
    [switch]$HttpOnly,
    [string]$MavenCache = 'target/biz-ux-01/maven'
)

$ErrorActionPreference = 'Stop'
$qa = (Resolve-Path 'docs/qa/BIZ-UX-01').Path
$buildDir = (Resolve-Path $Build).Path
$java = (Get-Command java.exe).Source
$env:JAVA_HOME = Split-Path (Split-Path $java)
$ErrorActionPreference = 'Continue'
$javaSettings = & $java -XshowSettings:properties -version 2>&1 | Out-String
$ErrorActionPreference = 'Stop'
if ($javaSettings -notmatch 'java.specification.version = 21') {
    throw 'Select Java 21 on PATH before running the fixture.'
}
if (!$MariaBin) {
    $installed = Get-ChildItem -Path "$env:ProgramFiles/MariaDB */bin/mariadbd.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if ($installed) { $MariaBin = $installed.DirectoryName }
}
if (!$MariaBin) { throw 'Pass the MariaDB installation bin directory.' }
if (!$MongoExe) {
    $installed = Get-ChildItem -Path "$env:ProgramFiles/MongoDB/Server/*/bin/mongod.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if ($installed) { $MongoExe = $installed.FullName }
}
if (!$ChromeExe) { $ChromeExe = "$env:ProgramFiles/Google/Chrome/Application/chrome.exe" }
$env:BIZ_UX_CHROME = $ChromeExe
if ($WebSocketModule) { $env:BIZ_UX_WS = (Resolve-Path $WebSocketModule).Path }
Get-ChildItem Env: | Where-Object Name -Match '^(SPRING_|DB_|MONGO|REDIS|MAIL|MOMO|OAUTH|GOOGLE_|GITHUB_|AI_|OLLAMA|APP_UPLOADS|AUTH_PUBLIC_URL|P01_MARIADB_TEST|P23_MONGO_TEST|AUTH_02_MIGRATION_FIXTURE|LOCAL_BOOTSTRAP_MIGRATION_FIXTURE|P21_DEMO_REPORT|ARCH_01_BSON_PHASE|DATA_01_MONGOD)' |
    ForEach-Object { Remove-Item -LiteralPath "Env:$($_.Name)" }

function Free-Port {
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try { return $listener.LocalEndpoint.Port } finally { $listener.Stop() }
}
function Wait-Port($process, $port) {
    for ($i = 0; $i -lt 75 -and !$process.HasExited; $i++) {
        try {
            $socket = [Net.Sockets.TcpClient]::new('127.0.0.1', $port)
            $socket.Dispose()
            return
        } catch { Start-Sleep -Milliseconds 250 }
    }
    throw "Fixture process did not listen on port $port."
}
function Sql($statement, $schema) {
    $options = $clientArgs
    if ($schema) { $options += "--database=$schema" }
    $statement | & "$MariaBin/mariadb.exe" @options
    if ($LASTEXITCODE) { throw 'Fixture SQL failed.' }
}

$scratch = Join-Path (Resolve-Path 'target/biz-ux-01').Path ("$Phase-fixture-" + [Guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $scratch
$dbPort = Free-Port
$mongoPort = if ($MongoExe) { Free-Port } else { 1 }
$webPort = Free-Port
$base = "http://127.0.0.1:$webPort"
$secret = [Guid]::NewGuid().ToString('N')
$code = 1
try {
    & "$MariaBin/mariadb-install-db.exe" "--datadir=$scratch/db" "--password=$secret" "--port=$dbPort" --silent | Out-Null
    if ($LASTEXITCODE) { throw 'MariaDB initialization failed.' }
    $db = Start-Process "$MariaBin/mariadbd.exe" -WindowStyle Hidden -PassThru -ArgumentList @(
        ('--defaults-file="' + (Join-Path $scratch 'db/my.ini') + '"'), '--bind-address=127.0.0.1', '--console'
    ) -RedirectStandardOutput "$scratch/db.log" -RedirectStandardError "$scratch/db-error.log"
    Wait-Port $db $dbPort
    if ($MongoExe) {
        $null = New-Item -ItemType Directory "$scratch/mongo"
        $mongo = Start-Process $MongoExe -WindowStyle Hidden -PassThru -ArgumentList @(
            '--bind_ip', '127.0.0.1', '--port', "$mongoPort", '--dbpath', ('"' + (Join-Path $scratch 'mongo') + '"')
        ) -RedirectStandardOutput "$scratch/mongo.log" -RedirectStandardError "$scratch/mongo-error.log"
        Wait-Port $mongo $mongoPort
    }
    $clientArgs = @('--no-defaults', '--skip-ssl', '--protocol=tcp', '--host=127.0.0.1',
        "--port=$dbPort", '--user=root', '--batch', '--skip-column-names', '--skip-print-query-on-error')
    $env:MYSQL_PWD = $secret
    $schema = switch ($Phase) {
        'migration' { 'beautyshop_demo_setup' }
        'integration' { 'auth_02_test' }
        'v5-drift' { 'biz_ux_v5_drift' }
        default { 'biz_ux_01_browser' }
    }
    Sql "CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/$schema"
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $secret
    $env:MONGODB_URI = "mongodb://127.0.0.1:$mongoPort/biz_ux_01_browser?serverSelectionTimeoutMS=500"
    $env:MONGODB_TEST_URI = "mongodb://127.0.0.1:$mongoPort/beautyshop_test_mongo?serverSelectionTimeoutMS=500"
    $env:REDIS_PORT = '1'
    $env:MAIL_HOST = '127.0.0.1'
    $env:MAIL_PORT = '1'
    $env:AUTH_PUBLIC_URL = $base
    $env:APP_UPLOADS_SKU_DIR = "$scratch/uploads"
    $repo = (Resolve-Path $MavenCache).Path
    if ($Phase -eq 'v5-drift') {
        $v5 = 'V5__nap_faq_beautyshop.sql'
        $currentMigrations = (Resolve-Path 'src/main/resources/db/migration').Path
        $oldMigrations = New-Item -ItemType Directory "$scratch/old-migrations"
        Get-ChildItem -LiteralPath $currentMigrations -File | Where-Object Name -Match '^V[1-4]__' |
            Copy-Item -Destination $oldMigrations.FullName
        $consoleEncoding = [Console]::OutputEncoding
        try {
            [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
            $oldSql = & git show "c79983a^:src/main/resources/db/migration/$v5"
            if ($LASTEXITCODE -or !$oldSql) { throw 'Historical V5 source is unavailable.' }
            [IO.File]::WriteAllLines((Join-Path $oldMigrations.FullName $v5), [string[]]$oldSql, [Text.UTF8Encoding]::new($false))
        } finally { [Console]::OutputEncoding = $consoleEncoding }

        $lib = New-Item -ItemType Directory "$scratch/lib"
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $buildDir 'target/beautyshop-0.0.1-SNAPSHOT.jar'))
        try {
            foreach ($entry in $archive.Entries.Where({$_.FullName -like 'BOOT-INF/lib/*.jar'})) {
                [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path $lib.FullName $entry.Name))
            }
        } finally { $archive.Dispose() }
        $ErrorActionPreference = 'Continue'
        & $java -cp "$($lib.FullName)/*" "$qa/LegacyFlywayProbe.java" $oldMigrations.FullName $currentMigrations *> "$qa/v5-drift.log"
        $code = $LASTEXITCODE
        Get-Content "$qa/v5-drift.log" -Tail 3
    } elseif ($Phase -eq 'integration' -or $Phase -eq 'migration') {
        if ($Phase -eq 'integration') {
            Sql 'CREATE DATABASE auth_02_v5 CHARACTER SET utf8mb4; CREATE DATABASE auth_02_v6 CHARACTER SET utf8mb4;'
            $env:P01_MARIADB_TEST = 'true'
            if ($mongo) { $env:P23_MONGO_TEST = 'true' }
            $env:AUTH_02_MIGRATION_FIXTURE = 'true'
            $suites = '*_MariaDb_Test,SauMua_MongoDanhGia_Test,MongoDanhGia_Store_Test'
        } else {
            foreach ($suffix in @('v1','v2','v3','v4','v5','v6','unmanaged','drift')) {
                Sql "CREATE DATABASE beautyshop_demo_setup_$suffix CHARACTER SET utf8mb4;"
            }
            $env:LOCAL_BOOTSTRAP_MIGRATION_FIXTURE = 'true'
            $suites = 'LocalDatabaseMigration_MariaDb_Test'
        }
        if ($Tests) { $suites = $Tests }
        $ErrorActionPreference = 'Continue'
        $log = if ($EvidenceTag) { "$qa/maven-$Phase-$EvidenceTag.log" } else { "$qa/maven-$Phase.log" }
        & mvn.cmd -B -o -ntp "-Dmaven.repo.local=$repo" -f "$buildDir/pom.xml" "-Dtest=$suites" test *> $log
        $code = $LASTEXITCODE
        Select-String -Path $log -Pattern 'Tests run: |BUILD ' | Select-Object -Last 3 | ForEach-Object Line
    } else {
        $jar = Join-Path $buildDir 'target/beautyshop-0.0.1-SNAPSHOT.jar'
        if (!(Test-Path -LiteralPath $jar)) { throw 'Packaged fixture JAR is missing.' }
        $web = Start-Process $java -WindowStyle Hidden -PassThru -ArgumentList @(
            '-Xmx512m', '-jar', ('"' + $jar + '"'), "--server.port=$webPort", '--server.address=127.0.0.1',
            '--spring.profiles.active=dev', '--app.ai.enabled=false', '--app.payment.momo.enabled=false'
        ) -RedirectStandardOutput "$qa/$Phase-web.log" -RedirectStandardError "$scratch/web-error.log"
        Wait-Port $web $webPort
        $env:BIZ_UX_FIXTURE_PASS = [Guid]::NewGuid().ToString('N')
        @'
import org.springframework.security.crypto.bcrypt.BCrypt;
class FixtureHash {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw(System.getenv("BIZ_UX_FIXTURE_PASS"), BCrypt.gensalt()));
    }
}
'@ | Set-Content -Encoding ascii "$scratch/FixtureHash.java"
        $crypto = Get-ChildItem -Path "$repo/org/springframework/security/spring-security-crypto/*/spring-security-crypto-*.jar" |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        $hash = & $java -cp $crypto.FullName "$scratch/FixtureHash.java"
        if ($LASTEXITCODE) { throw 'Fixture hash generation failed.' }
        Sql @"
INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro) VALUES
('buyer@example.invalid', '$hash', 'Fixture Buyer', 'CUSTOMER'),
('other@example.invalid', '$hash', 'Other Fixture Buyer', 'CUSTOMER'),
('admin@example.invalid', '$hash', 'Fixture Admin', 'ADMIN');
INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Cham soc da', 'cham-soc-da');
INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Fixture Brand', 'fixture-brand');
INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan) VALUES
(1, 1, 'Fixture Serum', 'fixture-a'), (1, 1, 'Fixture Cream', 'fixture-b');
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho) VALUES
(1, 'FIXTURE-A', '50 ml', 100000, 10), (2, 'FIXTURE-B', '15 ml', 150000, 10),
(1, 'FIXTURE-A-SOLD', '100 ml', 220000, 0);
INSERT INTO dia_chi_nguoi_dung (ma_nguoi_dung, ten_nguoi_nhan, so_dien_thoai, dia_chi_chi_tiet, la_mac_dinh)
VALUES (1, 'Fixture Buyer', '0000000000', 'Fixture Address', TRUE);
"@ $schema
        $runner = if ($HttpOnly) { 'http-check.mjs' } else { 'browser-check.mjs' }
        & node "$qa/$runner" $base $scratch $Phase
        $code = $LASTEXITCODE
    }
} finally {
    foreach ($process in @($web, $mongo, $db)) {
        if ($process -and !$process.HasExited) { Stop-Process -Id $process.Id }
    }
}
exit $code
