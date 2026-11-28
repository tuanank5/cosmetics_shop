param([Parameter(Mandatory)][string]$Checkout, [Parameter(Mandatory)][string]$MavenRepo,
      [Parameter(Mandatory)][string]$EvidenceDirectory)
$runner = Get-Content -Encoding UTF8 -Raw "$PSScriptRoot/../QUALITY/final-review/run-mariadb.ps1"
if (!$runner.Contains('-Dtest=CheckoutIntegrityTest,')) { throw 'Integration suite changed' }
$suite = '-Dtest=CuaHang_MariaDb_Test,DocView_MariaDb_Test,HttpLoi_MariaDb_Test,SauMua_MongoDanhGia_Test,ThongKe_MariaDb_Test,Audit_MariaDb_Test,DanhMucSanPhamThuongHieu_MariaDb_Test,CheckoutIntegrityTest,'
$runner = $runner.Replace('-Dtest=CheckoutIntegrityTest,', $suite)
$runner = $runner.Replace("mongodb://127.0.0.1:1/auth_02?serverSelectionTimeoutMS=300&connectTimeoutMS=300", 'mongodb://127.0.0.1:33473/beautyshop_test_mongo')
$ErrorActionPreference = 'Stop'
$port = 33473
$probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
$probe.Start()
$probe.Stop()
$fixtureDir = New-Item -ItemType Directory -Path (Join-Path (Resolve-Path $Checkout) ('target/voucher-mongo-' + [guid]::NewGuid().ToString('N')))
$mongo = Start-Process (Get-Command mongod.exe).Source -PassThru -WindowStyle Hidden -ArgumentList @(
    '--dbpath', ('"' + $fixtureDir.FullName + '"'), '--bind_ip', '127.0.0.1', '--port', $port
) -RedirectStandardOutput "$fixtureDir/server.log" -RedirectStandardError "$fixtureDir/stderr.log"
try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        if ($mongo.HasExited) { throw 'Isolated MongoDB failed to start' }
        try {
            $client = [Net.Sockets.TcpClient]::new('127.0.0.1',$port)
            $client.Dispose()
            $ready = $true
            break
        } catch { Start-Sleep -Milliseconds 300 }
    }
    if (!$ready -or $mongo.HasExited) { throw 'Isolated MongoDB unavailable' }
    $env:P23_MONGO_TEST = 'true'
    & ([scriptblock]::Create($runner)) @PSBoundParameters
} finally {
    if (!$mongo.HasExited) { Stop-Process -Id $mongo.Id }
}
