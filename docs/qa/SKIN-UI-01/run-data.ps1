$ErrorActionPreference = 'Stop'
$build = 'target/skin-ui-01-verify'
$pom = "$build/pom.xml"
if (!(Test-Path -LiteralPath $pom)) { throw "Build the isolated Maven project first: $pom" }

$fixture = Get-Content -Raw -Encoding UTF8 'docs/qa/QA-01/run-data.ps1'
$fixture = $fixture.Replace('docs/qa/QA-01', 'docs/qa/SKIN-UI-01')
$fixture = $fixture.Replace('& mvn.cmd -o -B -ntp ', "& mvn.cmd -o -B -ntp -f `"$pom`" ")
$runner = Join-Path (Resolve-Path $build) 'run-data.ps1'
[IO.File]::WriteAllText($runner, $fixture)
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $runner -EvidenceTag data
exit $LASTEXITCODE
