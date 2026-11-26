param([switch]$Integration, [switch]$Baseline)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path .).Path
$scratch = Join-Path $root ('target/review-01-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $scratch
$java = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe'
$env:JAVA_HOME = Split-Path (Split-Path $java)
$maria = 'C:/Program Files/MariaDB 12.2/bin'
$dbPort = if ($Integration) { 33387 } else { 33386 }
$webPort = 18100
$mongoPort = if ($Integration) { 28007 } else { 28006 }
$site = "http://127.0.0.1:$webPort"
$ports = if ($Integration) { @($dbPort, $mongoPort) } else { @($dbPort, $webPort, $mongoPort) }
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
    & "$maria/mariadb.exe" @sql --execute='CREATE DATABASE review_01_test CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE) { throw 'MariaDB fixture schema creation failed' }

    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/review_01_test"
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
    $env:MONGODB_URI = "mongodb://127.0.0.1:$mongoPort/review_01_test"
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
        $env:DATA_01_MONGOD = (Get-Command mongod.exe).Source
        $suites = 'AdminDanhGiaPagesTest,MongoDanhGia_Store_Test,MongoDanhGiaStoreNghiTest,SauMuaDanhGiaServiceTest,SauMua_MongoDanhGia_Test,OAuth2Callback_MariaDb_Test'
        $mvnLog = "$scratch/integration.log"
        $savedPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=$suites" test *> $mvnLog
        $code = $LASTEXITCODE
        $ErrorActionPreference = $savedPreference
        $summary = Select-String $mvnLog -Pattern '^\[(INFO|WARNING|ERROR)\] Tests run:|^\[INFO\] BUILD '
        $summary.Line | Set-Content -Encoding utf8 docs/qa/REVIEW-01/integration-results.txt
        $summary.Line
        exit $code
    }
    $jar = if ($Baseline) { 'target/review-01-baseline.jar' } else { 'target/beautyshop-0.0.1-SNAPSHOT.jar' }
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
    if (!$ready) { throw 'REVIEW-01 fixture web failed to start' }

    $env:REVIEW01_FIXTURE_PASS = [guid]::NewGuid().ToString('N')
    @'
import org.springframework.security.crypto.bcrypt.BCrypt;
class FixtureHash {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw(System.getenv("REVIEW01_FIXTURE_PASS"), BCrypt.gensalt()));
    }
}
'@ | Set-Content -Encoding ascii "$scratch/FixtureHash.java"
    $crypto = "$env:USERPROFILE/.m2/repository/org/springframework/security/spring-security-crypto/6.5.11/spring-security-crypto-6.5.11.jar"
    $hash = & $java -cp $crypto "$scratch/FixtureHash.java"
    if ($LASTEXITCODE) { throw 'Fixture hash generation failed' }
    @"
INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro)
VALUES ('admin@example.invalid', '$hash', 'Fixture Admin', 'ADMIN'),
       ('buyer@example.invalid', NULL, 'Fixture Buyer', 'CUSTOMER');
INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Fixture Category', 'fixture-category');
INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Fixture Brand', 'fixture-brand');
INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
VALUES (1, 1, 'Fixture Serum', 'fixture-serum');
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
VALUES (1, 'FIXTURE-1', '30 ml', 100000, 10);
INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan,
    dia_chi_khi_dat, tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_giao_thanh_cong)
VALUES ('BS-QA-REVIEW', 2, 'Fixture Buyer', '000', 'Fixture Address', 2200000, 2200000, 'DELIVERED', 'COD', UTC_TIMESTAMP(6));
INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat, don_gia, so_luong)
SELECT 1, 1, 'FIXTURE-1', 'Fixture Serum', '30 ml', 100000, 1 FROM seq_1_to_22 ORDER BY seq;
"@ | & "$maria/mariadb.exe" @sql review_01_test
    if ($LASTEXITCODE) { throw 'REVIEW-01 SQL seed failed' }
    $reviewData = 1..22 | ForEach-Object {
        @{
            maChiTietDonHang = $_
            maDonHang = 1
            maNguoiDung = 2
            maSanPham = 1
            maBienThe = 1
            tenHienThi = 'Fixture Buyer'
            soSao = 4
            noiDung = "Fixture review $_"
            hienThi = $false
            ngayTao = @{ '$date' = '2026-11-01T00:00:00Z' }
            phienBan = 0
        } | ConvertTo-Json -Depth 3 -Compress
    }
    $reviewData | Set-Content -Encoding ascii "$scratch/reviews.jsonl"
    & mongoimport.exe "--uri=$env:MONGODB_URI" --collection=danh_gia "--file=$scratch/reviews.jsonl" --quiet
    if ($LASTEXITCODE) { throw 'REVIEW-01 Mongo seed failed' }
    $mode = if ($Baseline) { 'baseline' } else { 'current' }
    & node docs/qa/REVIEW-01/browser-check.mjs $site $scratch $mode $mongo.Id
    exit $LASTEXITCODE
} finally {
    if ($mongo -and !$mongo.HasExited) { Stop-Process -Id $mongo.Id }
    if ($web -and !$web.HasExited) { Stop-Process -Id $web.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
