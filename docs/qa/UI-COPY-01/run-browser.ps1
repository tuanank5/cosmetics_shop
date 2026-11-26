$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
Push-Location $root
try {
    $build = 'target/ui-copy-01'
    $jar = "$build/target/beautyshop-0.0.1-SNAPSHOT.jar"
    if (!(Test-Path -LiteralPath $jar)) { throw "Build the QA JAR first: $jar" }
    $shared = Get-Content -Raw -Encoding UTF8 'docs/qa/UI-01C/browser-check.mjs'
    $marker = $shared.IndexOf('  const rects = ')
    if ($marker -lt 0) { throw 'Could not find the shared Chrome harness preamble' }
    $checks = Get-Content -Raw -Encoding UTF8 "$PSScriptRoot/browser-checks.mjs"
    [IO.File]::WriteAllText("$root/$build/browser.mjs", $shared.Substring(0, $marker) + $checks)
    $fixture = Get-Content -Raw -Encoding UTF8 'docs/qa/UI-01C/run-fixture.ps1'
    $fixture = $fixture.Replace('target/beautyshop-0.0.1-SNAPSHOT.jar', $jar)
    $fixture = $fixture.Replace('docs/qa/UI-01C/browser-check.mjs', "$build/browser.mjs")
    [IO.File]::WriteAllText("$root/$build/run-browser.ps1", $fixture)
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$build/run-browser.ps1"
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
