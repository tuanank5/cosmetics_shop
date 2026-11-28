# Run from the isolated checkout containing the candidate JAR and the ORDER harness.
$runner = Get-Content -Raw -Encoding UTF8 docs/qa/VOUCHER/run-browser.ps1
$marker = "    Write-Output 'COD orders and attempts"
if (!$runner.Contains($marker)) { throw 'Voucher fixture entry changed' }
$order = @'
    & "$maria/mariadb.exe" @sql cart_01_test --execute="
        UPDATE bien_the_san_pham SET gia_ban=200000 WHERE sku='FIXTURE-A';
        UPDATE bien_the_san_pham SET dang_hoat_dong=false WHERE sku='FIXTURE-B';"
    if ($LASTEXITCODE) { throw 'Isolated SKU fixture update failed' }
    & node docs/qa/ORDER/browser.cjs $site
    if ($LASTEXITCODE) { throw 'ORDER browser failed' }
'@
& ([scriptblock]::Create($runner.Replace($marker, $order + [Environment]::NewLine + $marker)))
