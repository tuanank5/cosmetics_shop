# Reuse the PAYMENT seed with its owned MoMo attempt on a new loopback database.
$ErrorActionPreference = 'Stop'
$source = Get-Content -Raw -Encoding utf8 docs/qa/PAYMENT/run-order-detail.ps1
$entry = '& node docs/qa/PAYMENT/order-detail.cjs $site'
if (!$source.Contains($entry)) { throw 'PAYMENT fixture entry changed' }
$screen = @'
    & node docs/qa/UI-UAT/payment-screen.cjs $site
    if ($LASTEXITCODE) { throw 'MoMo mock browser setup failed' }
    & node docs/qa/UI-UAT/native.cjs $site $scratch payment
'@
$source = $source.Replace($entry,$screen)
$marker = '$entry = '
$enableMock = @'
$fixture = $fixture.Replace("'--app.payment.momo.enabled=false'", "'--app.payment.momo.enabled=true', '--app.payment.momo.mode=mock'").Replace('33381','33485').Replace('18096','18191')
'@
& ([scriptblock]::Create($source.Replace($marker,$enableMock + [Environment]::NewLine + $marker)))
