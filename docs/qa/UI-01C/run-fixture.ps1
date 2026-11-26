param([switch]$Baseline, [switch]$Integration)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path .).Path
$scratch = Join-Path $root ('target/ui-01c-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $scratch
$java = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe'
$env:JAVA_HOME = Split-Path (Split-Path $java)
$maria = 'C:/Program Files/MariaDB 12.2/bin'
$dbPort = if ($Baseline) { 33379 } else { 33378 }
$webPort = if ($Baseline) { 18094 } else { 18093 }
$site = "http://127.0.0.1:$webPort"
foreach ($port in @($dbPort, $webPort)) {
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
    & "$maria/mariadb.exe" @sql --execute='CREATE DATABASE ui_01c_test CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE) { throw 'MariaDB fixture schema creation failed' }

    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/ui_01c_test"
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $dbSecret
    $env:MONGODB_URI = 'mongodb://127.0.0.1:1/ui_01c_test?serverSelectionTimeoutMS=150'
    $env:MONGODB_TEST_URI = $env:MONGODB_URI
    $env:REDIS_PORT = '1'
    $env:MAIL_HOST = '127.0.0.1'
    $env:MAIL_PORT = '1'
    $env:MAIL_SMTP_AUTH = 'false'
    $env:MAIL_SMTP_STARTTLS = 'false'
    $env:AUTH_PUBLIC_URL = $site
    $env:APP_UPLOADS_SKU_DIR = "$scratch/uploads"
    if ($Integration) {
        $env:P01_MARIADB_TEST = 'true'
        $suites = 'XacThuc_MariaDb_Test,XacThucDaPhuongThuc_MariaDb_Test,DonHangCOD_MariaDb_Test,HttpLoi_MariaDb_Test,PhieuGiamGia_MariaDb_Test,AdminCatalogSecurityTest,AdminDashboardPagesTest,LoginRedirectTest'
        $mvnLog = "$scratch/integration.log"
        $savedPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=$suites" test *> $mvnLog
        $code = $LASTEXITCODE
        $ErrorActionPreference = $savedPreference
        $summary = Select-String $mvnLog -Pattern '^\[INFO\] .*Tests run:|^\[INFO\] BUILD |^\[ERROR\] Tests run:'
        $summary.Line | Set-Content -Encoding utf8 docs/qa/UI-01C/integration-results.txt
        $summary.Line
        exit $code
    }
    $jar = if ($Baseline) { 'target/ui-01c-before.jar' } else { 'target/beautyshop-0.0.1-SNAPSHOT.jar' }
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
    if (!$ready) { throw 'UI-01C fixture web failed to start' }

    if (!$Baseline) {
        $env:UI01C_FIXTURE_PASS = [guid]::NewGuid().ToString('N')
        @'
import org.springframework.security.crypto.bcrypt.BCrypt;
class FixtureHash {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw(System.getenv("UI01C_FIXTURE_PASS"), BCrypt.gensalt()));
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
INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_khi_dat, tam_tinh, phi_van_chuyen, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_tao) VALUES
('UI-01C-FIXTURE', 2, 'Fixture Recipient', '0000000000', 'Fixture Address', 100000, 25000, 125000, 'PENDING_CONFIRMATION', 'COD', UTC_TIMESTAMP());
INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat, don_gia, so_luong) VALUES
(1, 1, 'FIXTURE-A', 'Fixture A', 'Fixture A', 100000, 1);
"@ | & "$maria/mariadb.exe" @sql ui_01c_test
        if ($LASTEXITCODE) { throw 'UI-01C seed failed' }
    }
    $mode = if ($Baseline) { 'before' } else { 'after' }
    & node docs/qa/UI-01C/browser-check.mjs $site $scratch $mode
    exit $LASTEXITCODE
} finally {
    if ($web -and !$web.HasExited) { Stop-Process -Id $web.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
