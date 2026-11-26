$ErrorActionPreference = 'Stop'
$qa = 'docs/qa/SKIN-UI-01'
$shots = 'docs/ui/screenshots/SKIN-UI-01'
$build = 'target/skin-ui-01-verify'
$jar = "$build/target/beautyshop-0.0.1-SNAPSHOT.jar"
$scratch = "$build/browser"
$null = New-Item -ItemType Directory -Force -Path $scratch
if (!(Test-Path -LiteralPath $jar)) { throw "Build the QA JAR first: $jar" }

$browser = Get-Content -Raw -Encoding UTF8 'docs/qa/UI-01C/browser-check.mjs'
$marker = $browser.IndexOf('  const rects = ')
if ($marker -lt 0) { throw 'Could not find the shared Chrome harness preamble' }
$preamble = $browser.Substring(0, $marker).Replace('docs/ui/screenshots/UI-01C', $shots)
$checks = Get-Content -Raw -Encoding UTF8 "$qa/browser-checks.mjs"
[IO.File]::WriteAllText((Join-Path (Resolve-Path $scratch) 'browser.mjs'), $preamble + $checks)

$runner = Get-Content -Raw -Encoding UTF8 'docs/qa/UI-01C/run-fixture.ps1'
$runner = $runner.Replace('target/beautyshop-0.0.1-SNAPSHOT.jar', $jar)
$runner = $runner.Replace('docs/qa/UI-01C/browser-check.mjs', "$scratch/browser.mjs")
[IO.File]::WriteAllText((Join-Path (Resolve-Path $scratch) 'run.ps1'), $runner)
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$scratch/run.ps1"
exit $LASTEXITCODE
