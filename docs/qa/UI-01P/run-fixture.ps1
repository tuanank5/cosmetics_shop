param([switch]$Integration)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path .).Path
$scratch = Join-Path $root ('target/ui-01p-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $scratch
$java = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe'
$env:JAVA_HOME = Split-Path (Split-Path $java)
$maria = 'C:/Program Files/MariaDB 12.2/bin'
$dbPort = 33380
$webPort = 18095
$mongoPort = 27999
$site = "http://127.0.0.1:$webPort"
foreach ($port in @($dbPort, $webPort, $mongoPort)) {
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
    & "$maria/mariadb.exe" @sql --execute='CREATE DATABASE ui_01p_test CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE) { throw 'MariaDB fixture schema creation failed' }

    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/ui_01p_test"
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
    $env:MONGODB_URI = "mongodb://127.0.0.1:$mongoPort/ui_01p_test"
    $env:MONGODB_TEST_URI = "mongodb://127.0.0.1:$mongoPort/beautyshop_test_mongo"
    $env:REDIS_PORT = '1'
    $env:MAIL_HOST = '127.0.0.1'
    $env:MAIL_PORT = '1'
    $env:MAIL_SMTP_AUTH = 'false'
    $env:MAIL_SMTP_STARTTLS = 'false'
    $env:AUTH_PUBLIC_URL = $site
    $env:APP_UPLOADS_SKU_DIR = "$scratch/uploads"
    if ($Integration) {
        $env:P01_MARIADB_TEST = 'true'
        $env:P23_MONGO_TEST = 'true'
        $suites = 'CuaHang_MariaDb_Test,XacThuc_MariaDb_Test,SauMua_MongoDanhGia_Test,MongoDanhGia_Store_Test,AdminDanhGiaPagesTest,GiaoDienPagesTest'
        $mvnLog = "$scratch/integration.log"
        $savedPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=$suites" test *> $mvnLog
        $code = $LASTEXITCODE
        $ErrorActionPreference = $savedPreference
        $summary = Select-String $mvnLog -Pattern '^\[(INFO|WARNING|ERROR)\] Tests run:|^\[INFO\] BUILD '
        $summary.Line | Set-Content -Encoding utf8 docs/qa/UI-01P/integration-results.txt
        $summary.Line
        exit $code
    }
    $jar = 'target/beautyshop-0.0.1-SNAPSHOT.jar'
    $web = Start-Process $java -WindowStyle Hidden -PassThru -ArgumentList @(
        '-Xmx512m', '-jar', $jar, "--server.port=$webPort", '--server.address=127.0.0.1', '--spring.profiles.active=dev',
        '--app.ai.enabled=false', '--app.payment.momo.enabled=false'
    ) -RedirectStandardOutput "$scratch/web.log" -RedirectStandardError "$scratch/web-error.log"
    $ready = $false
    for ($i = 0; $i -lt 40 -and !$web.HasExited; $i++) {
        try {
            $ready = (Invoke-WebRequest -UseBasicParsing "$site/login" -TimeoutSec 3).StatusCode -eq 200
            if ($ready) { break }
        } catch { Start-Sleep -Milliseconds 700 }
    }
    if (!$ready) { throw 'UI-01P fixture web failed to start' }

    $env:UI01P_FIXTURE_PASS = [guid]::NewGuid().ToString('N')
    @'
import org.springframework.security.crypto.bcrypt.BCrypt;
class FixtureHash {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw(System.getenv("UI01P_FIXTURE_PASS"), BCrypt.gensalt()));
    }
}
'@ | Set-Content -Encoding ascii "$scratch/FixtureHash.java"
    $crypto = "$env:USERPROFILE/.m2/repository/org/springframework/security/spring-security-crypto/6.5.11/spring-security-crypto-6.5.11.jar"
    $hash = & $java -cp $crypto "$scratch/FixtureHash.java"
    if ($LASTEXITCODE) { throw 'Fixture hash generation failed' }
    @"
INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro, ngay_xac_minh_email) VALUES
('admin@example.invalid', '$hash', 'Fixture Admin', 'ADMIN', UTC_TIMESTAMP()),
('buyer@example.invalid', '$hash', 'Fixture Buyer', 'CUSTOMER', UTC_TIMESTAMP());
INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Fixture Group', 'fixture-group');
INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Fixture Brand', 'fixture-brand');
INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan, thanh_phan, huong_dan_su_dung, loai_da_phu_hop) VALUES
(1, 1, 'Fixture A', 'fixture-a', 'Fixture ingredients', 'Fixture instructions', 'Fixture skin'),
(1, 1, 'Fixture B', 'fixture-b', 'Fixture ingredients', 'Fixture instructions', 'Fixture skin');
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho) VALUES
(1, 'FIXTURE-A', 'Fixture A', 100000, 5),
(2, 'FIXTURE-B', 'Fixture B', 150000, 5);
INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan, loai_da_phu_hop)
SELECT 1, 1, CONCAT('Fixture ', seq), CONCAT('fixture-', seq), 'Fixture skin' FROM seq_3_to_9;
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
SELECT ma_san_pham, CONCAT('FIXTURE-', ma_san_pham), 'Fixture variant', 150000 + ma_san_pham, 5
FROM san_pham WHERE ma_san_pham >= 3;
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
VALUES (1, 'FIXTURE-A-SELECTED', 'Fixture Selected', 200000, 5);
INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, kich_hoat)
SELECT CONCAT('pager-', seq, '@example.invalid'), NULL, CONCAT('Pager User ', seq), FALSE FROM seq_1_to_16;
"@ | & "$maria/mariadb.exe" @sql ui_01p_test
    if ($LASTEXITCODE) { throw 'UI-01P SQL seed failed' }

    $reviewData = 1..43 | ForEach-Object {
        @{
            maChiTietDonHang = $_
            maDonHang = 1
            maNguoiDung = 2
            maSanPham = 1
            maBienThe = 1
            tenHienThi = 'Fixture'
            soSao = 4
            noiDung = "Fixture review $_"
            hienThi = $true
            ngayTao = @{ '$date' = '2026-11-01T00:00:00Z' }
            ngayDuyet = @{ '$date' = '2026-11-01T00:00:00Z' }
            phienBan = 0
        } | ConvertTo-Json -Depth 3 -Compress
    }
    $reviewData | Set-Content -Encoding ascii "$scratch/reviews.jsonl"
    & mongoimport.exe "--uri=$env:MONGODB_URI" --collection=danh_gia "--file=$scratch/reviews.jsonl" --quiet
    if ($LASTEXITCODE) { throw 'UI-01P Mongo seed failed' }
    & node docs/qa/UI-01P/browser-check.mjs $site $scratch
    exit $LASTEXITCODE
} finally {
    if ($mongo -and !$mongo.HasExited) { Stop-Process -Id $mongo.Id }
    if ($web -and !$web.HasExited) { Stop-Process -Id $web.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
