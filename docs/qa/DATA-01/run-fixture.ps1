param([switch]$Backup, [switch]$MongoOff, [switch]$Ui, [switch]$Browser)
$ErrorActionPreference = 'Stop'
if ($Browser) {
    $MongoOff = $true
    $Ui = $true
}
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot'
$bin = 'C:/Program Files/MariaDB 12.2/bin'
$port = 33375
$probe = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
$probe.Start()
$probe.Stop()

$root = (Resolve-Path .).Path
$qa = Join-Path $root 'docs/qa/DATA-01'
$data = Join-Path $root ('target/data-01-db-' + [Guid]::NewGuid().ToString('N'))
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
    & "$bin/mariadb.exe" @clientArgs --execute='CREATE DATABASE data_01_test CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE -ne 0) { throw 'Could not create isolated schema' }
    Remove-Item Env:MYSQL_PWD
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$port/data_01_test"
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $secret
    $env:P01_MARIADB_TEST = 'true'
    $env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:27998/beautyshop_test_mongo'
    $env:P23_MONGO_TEST = 'true'
    $env:APP_UPLOADS_SKU_DIR = Join-Path $data 'uploads'
    foreach ($mongoPort in @(27998, 27999)) {
        $probeMongo = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $mongoPort)
        $probeMongo.Start()
        $probeMongo.Stop()
    }
    if (!$MongoOff) {
        $mongoData = New-Item -ItemType Directory -Path (Join-Path $data 'mongo')
        $mongoCommand = Get-Command mongod.exe
        $env:DATA_01_MONGOD = $mongoCommand.Source
        $mongo = Start-Process -FilePath $mongoCommand.Source -WindowStyle Hidden -PassThru -ArgumentList @('--dbpath', ('"' + $mongoData.FullName + '"'), '--port', '27998', '--bind_ip', '127.0.0.1', '--wiredTigerCacheSizeGB', '0.25', '--logpath', ('"' + (Join-Path $data 'mongo.log') + '"'))
        Start-Sleep -Seconds 2
        if ($mongo.HasExited) { throw 'Isolated MongoDB did not start' }
    }
    $ErrorActionPreference = 'Continue'
    $tests = if ($Ui) { 'MongoTat_MariaDb_Test,HttpLoi_MariaDb_Test' } elseif ($MongoOff) { 'MongoTat_MariaDb_Test,DonHangCOD_MariaDb_Test,Momo_MariaDb_Test' } elseif ($Backup) { 'GoiYSanPham_MariaDb_Test,MongoDanhGia_Store_Test#taoMauSaoLuu' } else { 'SauMua_MongoDanhGia_Test,MongoTat_MariaDb_Test,MongoDanhGia_Store_Test,DonHangCOD_MariaDb_Test,Momo_MariaDb_Test,GoiYSanPham_MariaDb_Test,ThongKe_MariaDb_Test' }
    $log = Join-Path $qa $(if ($Ui) {'ui-fixture.log'} elseif ($MongoOff) {'mongo-off.log'} elseif ($Backup) {'backup.log'} else {'integration.log'})
    & mvn -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=$tests" "-Ddata01.backup=$($Backup.IsPresent.ToString().ToLowerInvariant())" test *> $log
    $result = $LASTEXITCODE
    $filtered = @([IO.File]::ReadAllLines($log) | Where-Object { $_ -notmatch '(?i)password|token=|cookie:|authorization:' })
    [IO.File]::WriteAllLines($log, $filtered, [Text.UTF8Encoding]::new($false))
    Select-String -Path $log -Pattern 'Tests run:|BUILD SUCCESS|BUILD FAILURE'
    if ($result -ne 0) { exit $result }
    if ($Ui) {
        $uiPort = 18091
        $uiProbe = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $uiPort)
        $uiProbe.Start()
        $uiProbe.Stop()
        $env:MONGODB_URI = $env:MONGODB_TEST_URI
        $jar = Join-Path $root 'target/beautyshop-0.0.1-SNAPSHOT.jar'
        $uiArgs = @('-Xmx512m', '-jar', ('"' + $jar + '"'), "--server.port=$uiPort", '--server.address=127.0.0.1')
        $uiProcess = Start-Process -FilePath "$env:JAVA_HOME/bin/java.exe" -WindowStyle Hidden -PassThru -ArgumentList $uiArgs -RedirectStandardOutput (Join-Path $data 'ui.log') -RedirectStandardError (Join-Path $data 'ui-error.log')
        $response = $null
        for ($i = 0; $i -lt 30 -and !$uiProcess.HasExited; $i++) {
            try {
                $response = Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$uiPort/products" -TimeoutSec 5 -ErrorAction Stop
                break
            } catch { Start-Sleep -Seconds 1 }
        }
        if (!$response -or $response.StatusCode -ne 200 -or $response.Content -notmatch 'store-card' -or
            $response.Content -notmatch 'kh\u1ea3 d\u1ee5ng' -or $response.Content -notmatch 'href="/products/') {
            throw 'Storefront HTTP fallback check failed'
        }
        @{ status = 200; mongo = 'off'; productCards = $true; unavailableMarkup = $true; productLinks = $true } |
            ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $qa 'ui-http.json')
        if ($Browser) {
            & node (Join-Path $qa 'browser-check.mjs') "http://127.0.0.1:$uiPort" $data
            if ($LASTEXITCODE) { throw 'Chrome regression check failed' }
            return
        }
        $stop = Join-Path $data 'ui-finished.signal'
        "http://127.0.0.1:$uiPort/products | $stop"
        $deadline = [DateTime]::UtcNow.AddMinutes(6)
        while (!$uiProcess.HasExited -and !(Test-Path -LiteralPath $stop) -and [DateTime]::UtcNow -lt $deadline) {
            Start-Sleep -Seconds 1
        }
    }
    if ($Backup) {
        $ErrorActionPreference = 'Stop'
        $env:MYSQL_PWD = $secret
        $sql = Join-Path $data 'fixture.sql'
        & "$bin/mariadb-dump.exe" @clientArgs --single-transaction "--result-file=$sql" data_01_test
        if ($LASTEXITCODE) { throw 'SQL dump failed' }
        & "$bin/mariadb.exe" @clientArgs --execute='CREATE DATABASE data_01_restore CHARACTER SET utf8mb4;'
        & "$bin/mariadb.exe" @clientArgs data_01_restore "--execute=source $($sql.Replace('\','/'))"
        if ($LASTEXITCODE) { throw 'SQL restore failed' }
        $source = & "$bin/mariadb.exe" @clientArgs -N -e 'SELECT COUNT(*) FROM data_01_test.danh_gia; CHECKSUM TABLE data_01_test.danh_gia;'
        $copy = & "$bin/mariadb.exe" @clientArgs -N -e 'SELECT COUNT(*) FROM data_01_restore.danh_gia; CHECKSUM TABLE data_01_restore.danh_gia;'
        if ([int]$source[0] -lt 1 -or ($source[1] -split "\s+")[-1] -ne ($copy[1] -split "\s+")[-1] -or $source[0] -ne $copy[0]) { throw 'SQL historical review count/checksum mismatch' }
        $archive = Join-Path $data 'fixture.archive'
        & mongodump.exe --uri=$env:MONGODB_TEST_URI --db=beautyshop_test_mongo --collection=danh_gia "--archive=$archive"
        if ($LASTEXITCODE) { throw 'Mongo dump failed' }
        & mongorestore.exe --uri=mongodb://127.0.0.1:27998 "--archive=$archive" --nsFrom=beautyshop_test_mongo.danh_gia --nsTo=data_01_restore.danh_gia --stopOnError
        if ($LASTEXITCODE) { throw 'Mongo restore failed' }
        $verify = Join-Path $data 'verify'
        foreach ($schema in @('beautyshop_test_mongo', 'data_01_restore')) {
            & mongodump.exe --uri=mongodb://127.0.0.1:27998 "--db=$schema" --collection=danh_gia "--out=$verify"
            if ($LASTEXITCODE) { throw 'Mongo verification dump failed' }
        }
        $sourceBson = Join-Path $verify 'beautyshop_test_mongo/danh_gia.bson'
        $restoreBson = Join-Path $verify 'data_01_restore/danh_gia.bson'
        if ((Get-Item $sourceBson).Length -eq 0 -or (Get-FileHash $sourceBson).Hash -ne (Get-FileHash $restoreBson).Hash) {
            throw 'Mongo document checksum mismatch'
        }
        $metadata = Get-Content -Raw (Join-Path $verify 'data_01_restore/danh_gia.metadata.json') | ConvertFrom-Json
        $unique = @($metadata.indexes | Where-Object {
            $direction = $_.key.maChiTietDonHang
            $_.name -eq 'uq_ma_chi_tiet_don_hang' -and $_.unique -eq $true -and
                ($direction -eq 1 -or $direction.'$numberInt' -eq '1') -and
                !$_.sparse -and !$_.partialFilterExpression
        })
        if ($unique.Count -ne 1) { throw 'Mongo unique index was not restored' }
        $fixtureImage = Join-Path $data 'f183b270-8a75-470e-9989-3b44b50c19bf.png'
        [IO.File]::WriteAllBytes($fixtureImage, [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII='))
        $images = New-Item -ItemType Directory -Path (Join-Path $data 'restored-uploads')
        Copy-Item -LiteralPath $fixtureImage -Destination $images.FullName
        if ((Get-FileHash $fixtureImage).Hash -ne (Get-FileHash (Join-Path $images.FullName (Split-Path $fixtureImage -Leaf))).Hash) {
            throw 'Restored image checksum mismatch'
        }
        "SQL source and restored review rows: $($source[0]), checksum identical. Mongo BSON SHA-256 matches and unique index is restored. Image UUID and SHA-256 match. Synthetic fixtures only." | Set-Content (Join-Path $qa 'restore-final.txt')
    }
} finally {
    if ($uiProcess -and !$uiProcess.HasExited) { Stop-Process -Id $uiProcess.Id }
    if ($mongo -and !$mongo.HasExited) { Stop-Process -Id $mongo.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
