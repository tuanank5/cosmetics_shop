param([switch]$Baseline, [switch]$Measure, [switch]$Browser)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path .).Path
$scratch = Join-Path $root ('target/dto-01-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $scratch
$java = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe'
$env:JAVA_HOME = Split-Path (Split-Path $java)
$maria = 'C:/Program Files/MariaDB 12.2/bin'
$dbPort = if ($Browser) { 33389 } else { 33388 }
$webPort = 18101
$site = "http://127.0.0.1:$webPort"
$ports = if ($Browser) { @($dbPort, $webPort) } else { @($dbPort) }
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
    & "$maria/mariadb.exe" @sql --execute='CREATE DATABASE dto_01_test CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE) { throw 'MariaDB fixture schema creation failed' }

    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/dto_01_test"
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $dbSecret
    $env:MONGODB_URI = 'mongodb://127.0.0.1:1/dto_01_test?serverSelectionTimeoutMS=150'
    $env:MONGODB_TEST_URI = $env:MONGODB_URI
    $env:REDIS_PORT = '1'
    $env:MAIL_HOST = '127.0.0.1'
    $env:MAIL_PORT = '1'
    $env:MAIL_SMTP_AUTH = 'false'
    $env:MAIL_SMTP_STARTTLS = 'false'
    $env:AUTH_PUBLIC_URL = $site
    $env:APP_UPLOADS_SKU_DIR = "$scratch/uploads"
    if (!$Browser) {
        $env:P01_MARIADB_TEST = 'true'
        $suites = if ($Baseline -or $Measure) { 'DocView_MariaDb_Test' } else {
            'GiaoDienPagesTest,CheckoutPagesTest,CheckoutIntegrityTest,AdminCatalogSecurityTest,CuaHang_MariaDb_Test,BienTheSanPham_MariaDb_Test,DanhMucSanPhamThuongHieu_MariaDb_Test,DonHangCOD_MariaDb_Test,XacThuc_MariaDb_Test,XacThucDaPhuongThuc_MariaDb_Test,OAuth2Callback_MariaDb_Test'
        }
        $mvnLog = "$scratch/integration.log"
        $savedPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=$suites" "-Ddto01.baseline=$($Baseline.IsPresent)" test *> $mvnLog
        $code = $LASTEXITCODE
        $ErrorActionPreference = $savedPreference
        $name = if ($Baseline) { 'query-before.txt' } elseif ($Measure) { 'query-after.txt' } else { 'integration-results.txt' }
        $summary = Select-String $mvnLog -Pattern '^\[(INFO|WARNING|ERROR)\] Tests run:|^\[INFO\] BUILD |^\[DTO-01\]'
        $summary.Line | Set-Content -Encoding utf8 "docs/qa/DTO-01/$name"
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
    if (!$ready) { throw 'DTO-01 fixture web failed to start' }

    $env:DTO01_FIXTURE_PASS = [guid]::NewGuid().ToString('N')
    @'
import org.springframework.security.crypto.bcrypt.BCrypt;
class FixtureHash {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw(System.getenv("DTO01_FIXTURE_PASS"), BCrypt.gensalt()));
    }
}
'@ | Set-Content -Encoding ascii "$scratch/FixtureHash.java"
    $crypto = "$env:USERPROFILE/.m2/repository/org/springframework/security/spring-security-crypto/6.5.11/spring-security-crypto-6.5.11.jar"
    $hash = & $java -cp $crypto "$scratch/FixtureHash.java"
    if ($LASTEXITCODE) { throw 'Fixture hash generation failed' }
    @"
INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten)
VALUES ('buyer@example.invalid', '$hash', 'Fixture Buyer'), ('other@example.invalid', NULL, 'Fixture');
INSERT INTO dia_chi_nguoi_dung (ma_nguoi_dung,ten_nguoi_nhan,so_dien_thoai,dia_chi_chi_tiet,la_mac_dinh)
VALUES (1, 'Fixture Buyer', '000', 'Fixture address', TRUE), (2, 'Fixture', '000', 'Other fixture address', TRUE);
INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Fixture Category', 'fixture-category');
INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Fixture Brand', 'fixture-brand');
INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan) VALUES (1, 1, 'Fixture Serum', 'fixture-serum');
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho) VALUES (1, 'FIXTURE-1', '30 ml', 100000, 10);
INSERT INTO gio_hang (ma_nguoi_dung) VALUES (1);
INSERT INTO chi_tiet_gio_hang (ma_gio_hang, ma_bien_the, so_luong) VALUES (1, 1, 1);
"@ | & "$maria/mariadb.exe" @sql dto_01_test
    if ($LASTEXITCODE) { throw 'DTO-01 seed failed' }
    & node docs/qa/DTO-01/browser-check.mjs $site $scratch
    exit $LASTEXITCODE
} finally {
    if ($web -and !$web.HasExited) { Stop-Process -Id $web.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
