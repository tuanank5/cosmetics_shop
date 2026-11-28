param([Parameter(Mandatory)][string]$Checkout, [Parameter(Mandatory)][string]$OutputDirectory,
      [string]$SourceManifest, [switch]$DiagnosticsOnly, [switch]$BadgeReview)
$ErrorActionPreference = 'Stop'
$env:PYTHONDONTWRITEBYTECODE = '1'
$env:PERF_QA = $PSScriptRoot
$env:PERF_OUT = (New-Item -ItemType Directory $OutputDirectory -Force).FullName
if ($SourceManifest) { $manifest = Get-Content -Raw (Resolve-Path $SourceManifest) | ConvertFrom-Json }
Set-Location (Resolve-Path $Checkout)
if ((git rev-parse HEAD) -ne 'e33e4e3e72e23852b8dcc60623da822acd32cae7') { throw 'Unexpected parent' }
$changed = @(git diff HEAD --name-only -- src pom.xml)
if (($changed -join '|') -ne (@($manifest | ForEach-Object { $_.path }) -join '|') -or
    (git ls-files --others --exclude-standard -- src pom.xml)) { throw 'Unexpected source paths' }
foreach ($f in $manifest) {
    if ((Get-FileHash -LiteralPath $f.path).Hash -ne $f.sha256) { throw 'Unexpected source content' }
}
if (!(Test-Path target/beautyshop-0.0.1-SNAPSHOT.jar)) { throw 'Package checkout first' }
# Reuse the pinned login fixture up to BCrypt generation, then seed only PERF rows.
$base = Get-Content -Raw -Encoding UTF8 docs/qa/CART-01/run-fixture.ps1
$seedStart = $base.IndexOf('    @"')
$cleanup = $base.LastIndexOf('} finally {')
if ($seedStart -lt 0 -or $cleanup -lt $seedStart) { throw 'Unexpected CART fixture layout' }
$setup = $base.Substring(0, $seedStart).Replace('33381','33516').Replace('18096','18516').Replace('cart_01_test','perf_test')
$setup = $setup.Replace('C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe', "$env:JAVA_HOME/bin/java.exe")
$setup = $setup.Replace('mongodb://127.0.0.1:1/', 'mongodb://127.0.0.1:28516/')
$setup = $setup.Replace("'--bind-address=127.0.0.1', '--console'", "'--bind-address=127.0.0.1', '--innodb-buffer-pool-size=128M', '--console'")
$setup = $setup.Replace("'-Xmx512m'", "'-Xms256m','-Xmx512m','-XX:+UseG1GC','-Xlog:gc:file=target/perf-gc.log'")
$setup = $setup.Replace("'--app.ai.enabled=false'", "'--spring.thymeleaf.cache=true','--app.ai.enabled=false'")
$measure = @'
    (Get-Content -Raw "$env:PERF_QA/fixture.sql").Replace('__BCRYPT__', $hash) | & "$maria/mariadb.exe" @sql perf_test
    if ($LASTEXITCODE) { throw 'Fixture seed failed' }
    & python "$env:PERF_QA/images.py" $env:APP_UPLOADS_SKU_DIR
    if ($LASTEXITCODE) { throw 'Image fixture failed' }
    $env:PERF_DB_PORT = "$dbPort"
    $env:PERF_SITE = $site
    $env:PERF_DB_CLI = "$maria/mariadb.exe"
    if (!$DiagnosticsOnly) {
        $cpuBefore = $web.TotalProcessorTime.TotalSeconds
        & python "$env:PERF_QA/http_bench.py"
        if ($LASTEXITCODE) { throw 'HTTP benchmark failed' }
        $web.Refresh()
        [ordered]@{jvm_cpu_seconds=$web.TotalProcessorTime.TotalSeconds-$cpuBefore; jvm_peak_rss_bytes=$web.PeakWorkingSet64;
            db_peak_rss_bytes=(Get-Process -Id $db.Id).PeakWorkingSet64} | ConvertTo-Json | Set-Content "$env:PERF_OUT/resources.json"
        Get-Content target/perf-gc.log | Set-Content "$env:PERF_OUT/gc.txt"
        & node "$env:PERF_QA/browser.cjs" $site $env:PERF_OUT
        if ($LASTEXITCODE) { throw 'Browser benchmark failed' }
    }
    & python "$env:PERF_QA/diagnostics.py"
    if ($LASTEXITCODE) { throw 'SQL diagnostics failed' }
    & node "$env:PERF_QA/cart-webkit.cjs" $site $env:PERF_OUT
    if ($LASTEXITCODE) { throw 'Cart and checkout browser regression failed' }
'@
if ($BadgeReview) {
    # Only the new private PERF schema is modified, after the benchmark checks.
    $measure += [Environment]::NewLine + @'
    & "$maria/mariadb.exe" @sql perf_test --execute="UPDATE bien_the_san_pham SET so_luong_trong_kho=0 WHERE ma_bien_the=3;
      INSERT INTO phieu_giam_gia(ma_ap_dung,loai_giam_gia,gia_tri_giam,don_toi_thieu,gioi_han_su_dung,bat_dau_luc,ket_thuc_luc)
      VALUES ('PFREVIEW','FIXED',10000,0,5,UTC_TIMESTAMP()-INTERVAL 1 DAY,UTC_TIMESTAMP()+INTERVAL 1 DAY)"
    if ($LASTEXITCODE) { throw 'Badge fixture failed' }
    & node "$env:PERF_QA/final-review/badge.cjs" $site $env:PERF_OUT
    if ($LASTEXITCODE) { throw 'Badge regression failed' }
'@
}
$portCheck = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,28516)
$portCheck.Start()
$portCheck.Stop()
$dir = (New-Item -Type Directory ("target/perf-mongo-" + [guid]::NewGuid())).FullName
$mongo = Start-Process (Get-Command mongod.exe).Source -PassThru -WindowStyle Hidden -ArgumentList @(
    '--dbpath',('"' + $dir + '"'),'--bind_ip','127.0.0.1','--port','28516','--wiredTigerCacheSizeGB','0.25',
    '--logpath',('"' + "$dir/mongo.log" + '"'))
try {
    if ($mongo.HasExited) { throw 'Mongo fixture startup failed' }
    & ([scriptblock]::Create($setup + $measure + [Environment]::NewLine + $base.Substring($cleanup)))
} finally {
    if (!$mongo.HasExited) { Stop-Process -Id $mongo.Id }
}
