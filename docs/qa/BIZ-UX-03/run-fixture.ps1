param(
    [Parameter(Mandatory)][ValidateSet('http','integration','manual')][string]$Phase,
    [string]$MariaBin,
    [string]$MongoExe
)

$ErrorActionPreference = 'Stop'
$qa = (Resolve-Path 'docs/qa/BIZ-UX-03').Path
$httpCheck = (Resolve-Path 'docs/qa/BIZ-UX-02/http-check.mjs').Path
$work = (Resolve-Path 'target/biz-ux-03').Path
$build = "$work/current"
$java = (Get-Command java.exe).Source
$env:JAVA_HOME = Split-Path (Split-Path $java)
if (!$MariaBin) {
    $MariaBin = (Get-ChildItem "$env:ProgramFiles/MariaDB */bin/mariadbd.exe" | Sort-Object FullName -Descending | Select-Object -First 1).DirectoryName
}
if (!$MongoExe) {
    $MongoExe = (Get-ChildItem "$env:ProgramFiles/MongoDB/Server/*/bin/mongod.exe" | Sort-Object FullName -Descending | Select-Object -First 1).FullName
}
if (!$MariaBin -or !$MongoExe) { throw 'MariaDB or MongoDB executable not found.' }
$jar = "$build/target/beautyshop-0.0.1-SNAPSHOT.jar"
if ($Phase -ne 'integration' -and !(Test-Path -LiteralPath $jar)) { throw 'Build the fixture JAR first.' }

Get-ChildItem Env: | Where-Object Name -Match '^(SPRING_|DB_|MONGO|REDIS|MAIL|MOMO|OAUTH|GOOGLE_|GITHUB_|AI_|OLLAMA|APP_UPLOADS|AUTH_PUBLIC_URL|P01_|P23_|AUTH_02_|LOCAL_BOOTSTRAP_|P21_|ARCH_01_|DATA_01_)' |
    ForEach-Object { [Environment]::SetEnvironmentVariable($_.Name, $null, 'Process') }
$dir = (New-Item -ItemType Directory -Path (Join-Path $work ($Phase + '-' + [Guid]::NewGuid().ToString('N')))).FullName

function Free-Port {
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try { return $listener.LocalEndpoint.Port } finally { $listener.Stop() }
}
function Wait-Port($process, $port) {
    for ($i = 0; $i -lt 160 -and !$process.HasExited; $i++) {
        try {
            $client = [Net.Sockets.TcpClient]::new('127.0.0.1', $port)
            $client.Dispose()
            return
        } catch { Start-Sleep -Milliseconds 250 }
    }
    throw "Fixture did not open port $port."
}
function Sql($statement, $database) {
    $options = $argsSql
    if ($database) { $options += "--database=$database" }
    $statement | & "$MariaBin/mariadb.exe" @options
    if ($LASTEXITCODE) { throw 'Fixture SQL failed.' }
}

$dbPort = Free-Port
$mongoPort = Free-Port
$webPort = Free-Port
$origin = "http://127.0.0.1:$webPort"
$secret = [Guid]::NewGuid().ToString('N')
$schema = 'biz_ux_03'
$code = 1
try {
    if ($Phase -eq 'manual') {
        $manualPassword = Read-Host 'Chọn mật khẩu tạm cho hai tài khoản QA, ít nhất 12 ký tự' -AsSecureString
        $fixturePassword = [Net.NetworkCredential]::new('', $manualPassword).Password
        $manualPassword.Dispose()
        if ($fixturePassword.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($fixturePassword) -gt 72) {
            throw 'Mật khẩu QA cần ít nhất 12 ký tự và tối đa 72 byte UTF-8.'
        }
    } elseif ($Phase -eq 'http') {
        $fixturePassword = [Guid]::NewGuid().ToString('N')
    }
    & "$MariaBin/mariadb-install-db.exe" "--datadir=$dir/db" "--password=$secret" "--port=$dbPort" --silent | Out-Null
    if ($LASTEXITCODE) { throw 'MariaDB initialization failed.' }
    $db = Start-Process "$MariaBin/mariadbd.exe" -WindowStyle Hidden -PassThru -ArgumentList @(
        ('--defaults-file="' + $dir + '/db/my.ini"'), '--bind-address=127.0.0.1', '--console'
    ) -RedirectStandardOutput "$dir/db.log" -RedirectStandardError "$dir/db-error.log"
    Wait-Port $db $dbPort
    $null = New-Item -ItemType Directory "$dir/mongo"
    $mongo = Start-Process $MongoExe -WindowStyle Hidden -PassThru -ArgumentList @(
        '--bind_ip', '127.0.0.1', '--port', "$mongoPort", '--dbpath', ('"' + $dir + '/mongo"')
    ) -RedirectStandardOutput "$dir/mongo.log" -RedirectStandardError "$dir/mongo-error.log"
    Wait-Port $mongo $mongoPort
    $argsSql = @('--no-defaults', '--skip-ssl', '--protocol=tcp', '--host=127.0.0.1', "--port=$dbPort",
        '--user=root', '--batch', '--skip-print-query-on-error')
    $env:MYSQL_PWD = $secret
    Sql "CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/$schema"
    $env:DB_USER = 'root'
    $env:DB_PASSWORD = $secret
    $env:MONGODB_URI = "mongodb://127.0.0.1:$mongoPort/$schema"
    $env:MONGODB_TEST_URI = "mongodb://127.0.0.1:$mongoPort/beautyshop_test_mongo"
    $env:REDIS_PORT = '1'
    $env:MAIL_HOST = '127.0.0.1'
    $env:MAIL_PORT = '1'
    $env:AUTH_PUBLIC_URL = $origin
    $env:APP_UPLOADS_SKU_DIR = "$dir/uploads"
    if ($Phase -eq 'integration') {
        $env:P01_MARIADB_TEST = 'true'
        $env:P23_MONGO_TEST = 'true'
        $tests = 'BienTheSanPham_MariaDb_Test,CuaHang_MariaDb_Test,DonHangCOD_MariaDb_Test,PhieuGiamGia_MariaDb_Test,ThongKe_MariaDb_Test,AdminOrderList_MariaDb_Test,DocView_MariaDb_Test,SauMua_MongoDanhGia_Test,XacThuc_MariaDb_Test,HttpLoi_MariaDb_Test'
        $ErrorActionPreference = 'Continue'
        & mvn.cmd -B -o -ntp "-Dmaven.repo.local=$work/maven" -f "$build/pom.xml" "-Dtest=$tests" test *> "$qa/maven-integration.log"
        $code = $LASTEXITCODE
        Select-String -Path "$qa/maven-integration.log" -Pattern 'Tests run: |BUILD ' | Select-Object -Last 2 | ForEach-Object Line
    } else {
        $web = Start-Process $java -PassThru -WindowStyle Hidden -ArgumentList @(
            '-Xmx512m', '-jar', ('"' + $jar + '"'), "--server.port=$webPort", '--server.address=127.0.0.1',
            '--spring.profiles.active=dev', '--app.ai.enabled=false', '--app.payment.momo.enabled=false'
        ) -RedirectStandardOutput "$dir/app.log" -RedirectStandardError "$dir/app-error.log"
        Wait-Port $web $webPort
        @'
import org.springframework.security.crypto.bcrypt.BCrypt;
class FixtureHash {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw(System.getenv("BIZ_UX02_PASSWORD"), BCrypt.gensalt()));
    }
}
'@ | Set-Content -Encoding ascii "$dir/FixtureHash.java"
        $bcrypt = Get-ChildItem "$work/maven/org/springframework/security/spring-security-crypto/*/spring-security-crypto-*.jar" |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        $env:BIZ_UX02_PASSWORD = $fixturePassword
        try {
            $hash = & $java -cp $bcrypt.FullName "$dir/FixtureHash.java"
            if ($LASTEXITCODE) { throw 'Could not create the fixture password hash.' }
        } finally { $env:BIZ_UX02_PASSWORD = $null }
        Sql @"
INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro) VALUES
('buyer@example.invalid', '$hash', 'Fixture Buyer', 'CUSTOMER'),
('admin@example.invalid', '$hash', 'Fixture Admin', 'ADMIN');
INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Serum', 'serum');
INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Fixture Brand', 'fixture-brand');
INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan, loai_da_phu_hop) VALUES
(1, 1, 'Fixture Serum with Long Product Name', 'fixture-serum', 'Da kho'),
(1, 1, 'Fixture Cream', 'fixture-cream', 'Da kho');
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho) VALUES
(1, 'QA-SERUM-30', '30 ml', 150000, 20),
(1, 'QA-SERUM-50', '50 ml', 250000, 0),
(2, 'QA-CREAM-25', '25 ml', 100000, 10);
INSERT INTO phieu_giam_gia
(ma_ap_dung, loai_giam_gia, gia_tri_giam, don_toi_thieu, gioi_han_su_dung, bat_dau_luc, ket_thuc_luc)
VALUES ('FIXTURE10', 'FIXED', 10000, 0, 10, UTC_TIMESTAMP() - INTERVAL 1 DAY, UTC_TIMESTAMP() + INTERVAL 1 DAY);
"@ $schema
        if ($Phase -eq 'manual') {
            $fixturePassword = $null
            $null = Invoke-WebRequest -UseBasicParsing "$origin/products"
            Write-Host "BeautyShop QA $origin"
            Write-Host 'Customer buyer@example.invalid | Admin admin@example.invalid'
            $null = Read-Host 'Giữ cửa sổ này mở khi kiểm. Nhấn Enter để dừng fixture'
            $code = 0
        } else {
            $legacyOutput = "$dir/docs/qa/BIZ-UX-02"
            $null = New-Item -ItemType Directory -Force $legacyOutput
            $env:BIZ_UX02_PASSWORD = $fixturePassword
            Push-Location -LiteralPath $dir
            try {
                & node $httpCheck $origin after
                $code = $LASTEXITCODE
            } finally { Pop-Location }
            Copy-Item -LiteralPath "$legacyOutput/after-http.json" -Destination "$qa/http-regression.json"
            Copy-Item -LiteralPath "$dir/app.log" -Destination "$qa/http-app.log"
            if ($code -eq 0) {
                $query = @'
SELECT
  (SELECT COUNT(*) FROM don_hang),
  (SELECT COUNT(*) FROM chi_tiet_don_hang),
  (SELECT tong_thanh_toan FROM don_hang LIMIT 1),
  (SELECT so_luong_trong_kho FROM bien_the_san_pham WHERE sku='QA-SERUM-30'),
  (SELECT so_luot_da_dung FROM phieu_giam_gia WHERE ma_ap_dung='FIXTURE10');
'@
                $row = (Sql $query $schema | Select-Object -Last 1) -split '\s+'
                $result = [ordered]@{
                    orders = [int]$row[0]
                    orderLines = [int]$row[1]
                    orderTotal = [int]$row[2]
                    skuStockAfter = [int]$row[3]
                    voucherUses = [int]$row[4]
                }
                $result | ConvertTo-Json | Set-Content -LiteralPath "$qa/cod-db-check.json" -Encoding utf8
                if (($row -join ',') -ne '1,1,165000,19,1') { throw 'Unexpected COD state after the concurrent HTTP test.' }
            }
        }
    }
} finally {
    $env:BIZ_UX02_PASSWORD = $null
    $env:MYSQL_PWD = $null
    foreach ($process in @($web,$mongo,$db)) {
        if ($process -and !$process.HasExited) { Stop-Process -Id $process.Id }
    }
}
exit $code
