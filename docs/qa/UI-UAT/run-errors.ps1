param([Parameter(Mandatory)][string]$MavenRepo,[Parameter(Mandatory)][string]$PlaywrightCore)
$ErrorActionPreference='Stop'
$root=(Resolve-Path .).Path
$case=(New-Item -ItemType Directory -Path (Join-Path $root ('target/error-ui-'+[guid]::NewGuid()))).FullName
Copy-Item -LiteralPath pom.xml -Destination $case
Copy-Item -Recurse -LiteralPath src -Destination $case
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'UiUatErrors_Test.java') -Destination (Join-Path $case 'src/test/java/com/beautyshop/integration')
$env:CART01_FIXTURE_PASS=[guid]::NewGuid().ToString('N')
$ev=(New-Item -Force -ItemType Directory (Join-Path $PSScriptRoot 'error-fixture')).FullName
$env:BASE_PLAYWRIGHT=(Resolve-Path $PlaywrightCore).Path
$repo=(Resolve-Path $MavenRepo).Path
$runner=Join-Path $root 'docs/qa/ACCOUNT/run-fixture.ps1'
$job=Start-Job -ArgumentList $runner,$case,$repo,$env:BASE_PLAYWRIGHT,$ev -ScriptBlock {
 param($runner,$case,$repo,$pw,$ev)
 $env:JAVA_TOOL_OPTIONS='-Dsec01.ui=true'
 & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $runner -Checkout $case -MavenRepo $repo -PlaywrightCore $pw -EvidenceDirectory $ev -Suite UiUatErrors_Test -SkipMigration
 if ($LASTEXITCODE) {throw 'HTTP dispatch fixture failed'}
}
$signal=Join-Path $case 'docs/qa/SEC-01/ui-finished.signal'
try {
 $portFile=Join-Path $case 'docs/qa/SEC-01/ui-port.txt'
 $limit=(Get-Date).AddMinutes(4)
 while (!(Test-Path -LiteralPath $portFile) -and $job.State -eq 'Running' -and (Get-Date) -lt $limit) {Start-Sleep -Milliseconds 400}
 if (!(Test-Path -LiteralPath $portFile)) {throw 'HTTP fixture did not publish its loopback port'}
 $port=[int](Get-Content -LiteralPath $portFile)
 $site="http://127.0.0.1:$port"
 & node docs/qa/UI-UAT/browser.cjs $site errors
 if ($LASTEXITCODE) {throw 'Error-page WebKit setup failed'}
 & node docs/qa/UI-UAT/native.cjs $site $case errors
 if ($LASTEXITCODE) {throw 'Error-page Chrome setup failed'}
} finally {
 if (Test-Path -LiteralPath (Split-Path $signal)) {Set-Content -LiteralPath $signal -Value 'finished'}
 $null=Wait-Job $job
 Receive-Job $job
 Remove-Job $job
}
