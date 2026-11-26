param([switch]$Integration)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path .).Path
$scratch = Join-Path $root ('target/wish-01-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $scratch
$java = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe'
$env:JAVA_HOME = Split-Path (Split-Path $java)
$maria = 'C:/Program Files/MariaDB 12.2/bin'
$dbPort = 33383
$webPort = 18098
$mongoPort = 28000
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
    & "$maria/mariadb.exe" @sql --execute='CREATE DATABASE wish_01_test CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE) { throw 'MariaDB fixture schema creation failed' }

    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/wish_01_test"
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
    $env:MONGODB_URI = "mongodb://127.0.0.1:$mongoPort/wish_01_test"
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
        $suites = 'SauMua_MongoDanhGia_Test,GiaoDienPagesTest,TrangChuPagesTest,OAuth2Callback_MariaDb_Test,XacThuc_MariaDb_Test,DanhMucSanPhamThuongHieu_MariaDb_Test'
        $mvnLog = "$scratch/integration.log"
        $savedPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=$suites" test *> $mvnLog
        $code = $LASTEXITCODE
        $ErrorActionPreference = $savedPreference
        $summary = Select-String $mvnLog -Pattern '^\[(INFO|WARNING|ERROR)\] Tests run:|^\[INFO\] BUILD '
        $summary.Line | Set-Content -Encoding utf8 docs/qa/WISH-01/integration-results.txt
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
    if (!$ready) { throw 'WISH-01 fixture web failed to start' }

    $env:WISH01_FIXTURE_PASS = [guid]::NewGuid().ToString('N')
    @'
import org.springframework.security.crypto.bcrypt.BCrypt;
class FixtureHash {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw(System.getenv("WISH01_FIXTURE_PASS"), BCrypt.gensalt()));
    }
}
'@ | Set-Content -Encoding ascii "$scratch/FixtureHash.java"
    $crypto = "$env:USERPROFILE/.m2/repository/org/springframework/security/spring-security-crypto/6.5.11/spring-security-crypto-6.5.11.jar"
    $hash = & $java -cp $crypto "$scratch/FixtureHash.java"
    if ($LASTEXITCODE) { throw 'Fixture hash generation failed' }
    @"
INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro)
VALUES ('buyer@example.invalid', '$hash', 'Fixture Buyer', 'CUSTOMER');
INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Fixture Category', 'fixture-category');
INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Fixture Brand', 'fixture-brand');
INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
VALUES (1, 1, 'Fixture Serum', 'fixture-serum');
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
VALUES (1, 'FIXTURE-1', '30 ml', 100000, 10), (1, 'FIXTURE-2', '50 ml', 150000, 5);
INSERT INTO gio_hang (ma_nguoi_dung) VALUES (1);
INSERT INTO chi_tiet_gio_hang (ma_gio_hang, ma_bien_the, so_luong) VALUES (1, 1, 7);
"@ | & "$maria/mariadb.exe" @sql wish_01_test
    if ($LASTEXITCODE) { throw 'WISH-01 SQL seed failed' }
    & node docs/qa/WISH-01/browser-check.mjs $site $scratch
    exit $LASTEXITCODE
} finally {
    if ($mongo -and !$mongo.HasExited) { Stop-Process -Id $mongo.Id }
    if ($web -and !$web.HasExited) { Stop-Process -Id $web.Id }
    if (!$db.HasExited) { Stop-Process -Id $db.Id }
}
