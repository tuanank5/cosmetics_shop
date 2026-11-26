$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$build = 'target/ui-copy-01'
Push-Location $root
try {
    $fixture = Get-Content -Raw -Encoding UTF8 'docs/qa/QA-01/run-data.ps1'
    $fixture = $fixture.Replace('docs/qa/QA-01', 'docs/qa/UI-COPY-01')
    $fixture = $fixture.Replace('New-Item -ItemType Directory -Path $evidence', 'New-Item -ItemType Directory -Force -Path $evidence')
    $fixture = $fixture.Replace('& mvn.cmd -o -B -ntp ', "& mvn.cmd -o -B -ntp -f $build/pom.xml ")
    $runner = Join-Path (Resolve-Path $build) 'run-data.ps1'
    [IO.File]::WriteAllText($runner, $fixture)
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $runner -EvidenceTag data
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
