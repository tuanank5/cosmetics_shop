param(
    [switch]$RestoreOnly,
    [ValidatePattern('^[a-zA-Z0-9-]+$')][string]$EvidenceTag
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path .).Path
$evidence = 'docs/qa/QA-01'
if ($EvidenceTag) {
    $evidence = Join-Path $evidence $EvidenceTag
    $null = New-Item -ItemType Directory -Path $evidence
}
$scratch = Join-Path $root ('target/qa-01-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $scratch
$java = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe'
$env:JAVA_HOME = Split-Path (Split-Path $java)
$maria = 'C:/Program Files/MariaDB 12.2/bin'
$dbPort = 33390
$mongoPort = 28008
$site = "http://127.0.0.1:18102"
$ports = @($dbPort, $mongoPort, 27999)
foreach ($port in $ports) {
    $probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
    $probe.Start()
    $probe.Stop()
}

$dbSecret = [guid]::NewGuid().ToString('N')
& "$maria/mariadb-install-db.exe" "--datadir=$scratch/db" "--password=$dbSecret" "--port=$dbPort" --silent | Out-Null
if ($LASTEXITCODE) { throw 'MariaDB fixture initialization failed' }
$db = Start-Process "$maria/mariadbd.exe" -WindowStyle Hidden -PassThru -ArgumentList @(
    ('--defaults-file="' + (Join-Path $scratch 'db/my.ini') + '"'), '--bind-address=127.0.0.1', '--console'
) -RedirectStandardOutput "$scratch/db.log" -RedirectStandardError "$scratch/db-error.log"
try {
    for ($i = 0; $i -lt 40; $i++) {
        try {
            $probe = [Net.Sockets.TcpClient]::new('127.0.0.1', $dbPort)
            $probe.Dispose()
            break
        } catch { Start-Sleep -Milliseconds 300 }
    }
    $sql = @('--skip-ssl', '--protocol=tcp', '--host=127.0.0.1', "--port=$dbPort", '--user=root')
    $env:MYSQL_PWD = $dbSecret
    & "$maria/mariadb.exe" @sql --execute='CREATE DATABASE auth_02_test CHARACTER SET utf8mb4; CREATE DATABASE auth_02_v5 CHARACTER SET utf8mb4; CREATE DATABASE auth_02_v6 CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE) { throw 'MariaDB fixture schema creation failed' }

    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/auth_02_test"
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $dbSecret
    $mongoPath = New-Item -Type Directory -Path "$scratch/mongo"
    $mongo = Start-Process (Get-Command mongod.exe).Source -WindowStyle Hidden -PassThru -ArgumentList @(
        '--dbpath', ('"' + $mongoPath.FullName + '"'), '--bind_ip', '127.0.0.1', '--port', $mongoPort
    ) -RedirectStandardOutput "$scratch/mongo.log" -RedirectStandardError "$scratch/mongo-error.log"
    for ($i = 0; $i -lt 40; $i++) {
        try {
            $probe = [Net.Sockets.TcpClient]::new('127.0.0.1', $mongoPort)
            $probe.Dispose()
            break
        } catch { Start-Sleep -Milliseconds 300 }
    }
    $env:MONGODB_URI = "mongodb://127.0.0.1:$mongoPort/beautyshop_test_mongo"
    $env:MONGODB_TEST_URI = "mongodb://127.0.0.1:$mongoPort/beautyshop_test_mongo"
    $env:REDIS_PORT = '1'
    $env:MAIL_HOST = '127.0.0.1'
    $env:MAIL_PORT = '1'
    $env:MAIL_SMTP_AUTH = 'false'
    $env:MAIL_SMTP_STARTTLS = 'false'
    $env:AUTH_PUBLIC_URL = $site
    $env:APP_UPLOADS_SKU_DIR = "$scratch/uploads"
    $env:P01_MARIADB_TEST = 'true'
    $env:P23_MONGO_TEST = 'true'
    $env:AUTH_02_MIGRATION_FIXTURE = 'true'
    $env:DATA_01_MONGOD = (Get-Command mongod.exe).Source
    Remove-Item Env:ARCH_01_BSON_PHASE,Env:P21_DEMO_REPORT -ErrorAction SilentlyContinue

    function Run-Tests($name, $arguments, [switch]$ReportFailure) {
        $log = Join-Path $scratch "$name.raw.log"
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" @arguments test *> $log
        $code = $LASTEXITCODE
        $summary = (Select-String $log -Pattern '^\[(INFO|WARNING|ERROR)\] Tests run:|^\[INFO\] BUILD ').Line
        $summary | Set-Content -Encoding utf8 "$evidence/$name.txt"
        $summary | Select-Object -Last 3 | Write-Host
        if ($code -and !$ReportFailure) { throw "$name failed (see filtered summary)" }
        if ($ReportFailure) { return $code }
    }

    $integrationExit = 0
    if ($RestoreOnly) {
        Run-Tests 'restore-sql-fixture' @('-Dtest=DonHangCOD_MariaDb_Test,GoiYSanPham_MariaDb_Test')
    } else {
        $integrationExit = Run-Tests 'integration' @('-Ddata01.backup=true') -ReportFailure
    }

    $env:ARCH_01_BSON_PHASE = 'new-roundtrip'
    $testMongo = $env:MONGODB_TEST_URI
    $env:MONGODB_URI = "mongodb://127.0.0.1:$mongoPort/arch_01_copy"
    $env:MONGODB_TEST_URI = $env:MONGODB_URI
    & mongoimport.exe "--uri=$env:MONGODB_URI" --collection=danh_gia --file=docs/qa/ARCH-01/legacy-fixture.jsonl --quiet
    if ($LASTEXITCODE) { throw 'Could not load the legacy BSON fixture' }
    Run-Tests 'bson' @('-Dtest=ReviewBsonCompatibilityTest', "-Dspring.data.mongodb.uri=$env:MONGODB_TEST_URI")
    Remove-Item Env:ARCH_01_BSON_PHASE
    $env:MONGODB_TEST_URI = $testMongo
    $env:MONGODB_URI = $testMongo
    Run-Tests 'backup-fixture' @('-Dtest=MongoDanhGia_Store_Test#taoMauSaoLuu', '-Ddata01.backup=true')

    $imageName = 'f183b270-8a75-470e-9989-3b44b50c19bf.png'
    $imageUrl = "/uploads/sku/$imageName"
    & "$maria/mariadb.exe" @sql auth_02_test --execute="UPDATE bien_the_san_pham SET url_hinh_anh = '$imageUrl' ORDER BY ma_bien_the LIMIT 1;"
    if ($LASTEXITCODE) { throw 'Could not link fixture SKU image' }
    $dump = Join-Path $scratch 'fixture.sql'
    & "$maria/mariadb-dump.exe" @sql --single-transaction "--result-file=$dump" auth_02_test
    if ($LASTEXITCODE) { throw 'SQL dump failed' }
    & "$maria/mariadb.exe" @sql --execute='CREATE DATABASE qa_01_restore CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE) { throw 'SQL restore schema creation failed' }
    & "$maria/mariadb.exe" @sql qa_01_restore "--execute=source $($dump.Replace('\','/'))"
    if ($LASTEXITCODE) { throw 'SQL restore failed' }
    foreach ($table in @('don_hang','lan_thanh_toan','danh_gia','bien_the_san_pham','phieu_giam_gia')) {
        $source = & "$maria/mariadb.exe" @sql -N -e "SELECT COUNT(*) FROM auth_02_test.$table; CHECKSUM TABLE auth_02_test.$table;"
        $copy = & "$maria/mariadb.exe" @sql -N -e "SELECT COUNT(*) FROM qa_01_restore.$table; CHECKSUM TABLE qa_01_restore.$table;"
        if ([int]$source[0] -lt 1 -or $source[0] -ne $copy[0] -or ($source[1] -split '\s+')[-1] -ne ($copy[1] -split '\s+')[-1]) {
            throw "Restore mismatch for $table"
        }
        $restoreEvidence += @("SQL $table : $($source[0]) rows, matching checksum")
    }
    $imageReferences = & "$maria/mariadb.exe" @sql -N -e "SELECT COUNT(*) FROM qa_01_restore.bien_the_san_pham WHERE url_hinh_anh = '$imageUrl';"
    if ([int]$imageReferences -ne 1) { throw 'Restored SKU reference missing' }

    $archive = Join-Path $scratch 'fixture.archive'
    & mongodump.exe "--uri=$env:MONGODB_TEST_URI" --collection=danh_gia "--archive=$archive" --quiet
    if ($LASTEXITCODE) { throw 'Mongo dump failed' }
    & mongorestore.exe "--uri=mongodb://127.0.0.1:$mongoPort" "--archive=$archive" --nsFrom=beautyshop_test_mongo.danh_gia --nsTo=qa_01_restore.danh_gia --stopOnError --quiet
    if ($LASTEXITCODE) { throw 'Mongo restore failed' }
    $verify = Join-Path $scratch 'verify'
    foreach ($schema in @('beautyshop_test_mongo','qa_01_restore')) {
        & mongodump.exe "--uri=mongodb://127.0.0.1:$mongoPort/$schema" --collection=danh_gia "--out=$verify" --quiet
        if ($LASTEXITCODE) { throw 'Mongo verification dump failed' }
    }
    $sourceBson = Join-Path $verify 'beautyshop_test_mongo/danh_gia.bson'
    $copyBson = Join-Path $verify 'qa_01_restore/danh_gia.bson'
    if ((Get-Item $sourceBson).Length -eq 0 -or (Get-FileHash $sourceBson).Hash -ne (Get-FileHash $copyBson).Hash) {
        throw 'Restored BSON differs from source'
    }
    $metadata = Get-Content -Raw (Join-Path $verify 'qa_01_restore/danh_gia.metadata.json') | ConvertFrom-Json
    $unique = @($metadata.indexes | Where-Object {
        $direction = $_.key.maChiTietDonHang
        $_.name -eq 'uq_ma_chi_tiet_don_hang' -and $_.unique -eq $true -and
            ($direction -eq 1 -or $direction.'$numberInt' -eq '1') -and !$_.sparse -and !$_.partialFilterExpression
    })
    if ($unique.Count -ne 1) { throw 'Could not verify restored unique review index' }
    $restoreEvidence += @('Mongo danh_gia: matching BSON checksum and unique maChiTietDonHang index')

    $fixtureImage = Join-Path $scratch $imageName
    [IO.File]::WriteAllBytes($fixtureImage, [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII='))
    $images = New-Item -ItemType Directory -Path (Join-Path $scratch 'restored-uploads')
    Copy-Item -LiteralPath $fixtureImage -Destination $images.FullName
    if ((Get-FileHash $fixtureImage).Hash -ne (Get-FileHash (Join-Path $images.FullName $imageName)).Hash) {
        throw 'Restored fixture image differs from source'
    }
    $restoreEvidence += @('Image fixture: matching UUID filename, SHA-256 and SQL SKU reference')
    $restoreEvidence | Set-Content -Encoding utf8 "$evidence/restore.txt"
    $restoreEvidence
    exit $integrationExit
} finally {
    if ($mongo -and !$mongo.HasExited) { Stop-Process -Id $mongo.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
