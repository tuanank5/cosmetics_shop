param([switch]$Baseline)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot'
$bin = 'C:/Program Files/MariaDB 12.2/bin'
$port = 33376
$probe = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
$probe.Start()
$probe.Stop()

$root = (Resolve-Path .).Path
$qa = Join-Path $root 'docs/qa/ARCH-01'
$data = Join-Path $root ('target/arch-01-db-' + [Guid]::NewGuid().ToString('N'))
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
    $env:P23_MONGO_TEST = 'true'
    $env:AUTH_02_MIGRATION_FIXTURE = 'true'
    $env:APP_UPLOADS_SKU_DIR = Join-Path $data 'uploads'
    $mongoPort = 27997
    $probeMongo = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $mongoPort)
    $probeMongo.Start()
    $probeMongo.Stop()
    $mongoRoot = New-Item -ItemType Directory -Path (Join-Path $data 'mongo')
    $mongod = (Get-Command mongod.exe).Source
    $env:DATA_01_MONGOD = $mongod
    $mongo = Start-Process -FilePath $mongod -WindowStyle Hidden -PassThru -ArgumentList @('--dbpath', ('"' + $mongoRoot.FullName + '"'), '--port', $mongoPort, '--bind_ip', '127.0.0.1', '--wiredTigerCacheSizeGB', '0.25', '--logpath', ('"' + (Join-Path $data 'mongo.log') + '"'))
    Start-Sleep -Seconds 2
    if ($mongo.HasExited) { throw 'Isolated Mongo did not start' }
    $uri = "mongodb://127.0.0.1:$mongoPort"
    $old = Join-Path $root 'target/arch-01-before'
    function Run-Maven($directory, $name, [string[]] $mavenArgs) {
        $log = Join-Path $qa "$name.log"
        Push-Location -LiteralPath $directory
        try {
            $ErrorActionPreference = 'Continue'
            & mvn -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" @mavenArgs *> $log
            $code = $LASTEXITCODE
            $safe = @([IO.File]::ReadAllLines($log) | Where-Object { $_ -notmatch '(?i)password|token=|cookie:|authorization:' })
            [IO.File]::WriteAllLines($log, $safe)
            Select-String -Path $log -Pattern 'Tests run:|BUILD SUCCESS|BUILD FAILURE|\[ERROR\]'
            if ($code) { throw "Maven failed: $name ($code)" }
        } finally { Pop-Location }
    }

    $env:ARCH_01_BSON_PHASE = 'old-write'
    $env:MONGODB_TEST_URI = "$uri/arch_01_source"
    Run-Maven $old 'old-write' @('-Dtest=ReviewBsonCompatibilityTest','test')
    $dump = Join-Path $data 'dump'
    & mongodump.exe "--uri=$uri" --db=arch_01_source --collection=danh_gia "--out=$dump" --quiet
    if ($LASTEXITCODE) { throw 'Mongo dump failed' }
    $evidence = Join-Path $qa 'legacy-fixture.jsonl'
    & bsondump.exe "--outFile=$evidence" (Join-Path $dump 'arch_01_source/danh_gia.bson') --quiet
    if ($LASTEXITCODE) { throw 'BSON evidence export failed' }
    if ($Baseline) { return }
    & mongorestore.exe "--uri=$uri" "--dir=$dump" --nsFrom=arch_01_source.danh_gia --nsTo=arch_01_copy.danh_gia --stopOnError --quiet
    if ($LASTEXITCODE) { throw 'Mongo restore to fresh namespace failed' }
    $env:MONGODB_TEST_URI = "$uri/arch_01_copy"
    $env:ARCH_01_BSON_PHASE = 'new-roundtrip'
    Run-Maven $root 'new-roundtrip' @('-Dtest=ReviewBsonCompatibilityTest','test')
    $env:ARCH_01_BSON_PHASE = 'old-read'
    Run-Maven $old 'old-read' @('-Dtest=ReviewBsonCompatibilityTest','test')
    $copyDump = Join-Path $data 'verified-copy-dump'
    & mongodump.exe "--uri=$uri" --db=arch_01_copy --collection=danh_gia "--out=$copyDump" --quiet
    if ($LASTEXITCODE) { throw 'Verified BSON export failed' }
    & bsondump.exe "--outFile=$(Join-Path $qa 'roundtrip-fixture.jsonl')" (Join-Path $copyDump 'arch_01_copy/danh_gia.bson') --quiet
    if ($LASTEXITCODE) { throw 'Verified BSON evidence export failed' }
    Remove-Item Env:ARCH_01_BSON_PHASE
    $env:MONGODB_TEST_URI = "$uri/beautyshop_test_mongo"
    Run-Maven $root 'g3-integration' @('-Dtest=XacThucMigration_MariaDb_Test,XacThucDaPhuongThuc_MariaDb_Test,XacThuc_MariaDb_Test,OAuth2Callback_MariaDb_Test,DanhMucSanPhamThuongHieu_MariaDb_Test,BienTheSanPham_MariaDb_Test,CuaHang_MariaDb_Test,MongoDanhGia_Store_Test,SauMua_MongoDanhGia_Test,DonHangCOD_MariaDb_Test,Momo_MariaDb_Test,HttpLoi_MariaDb_Test','test')
} finally {
    if ($mongo -and !$mongo.HasExited) { Stop-Process -Id $mongo.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
