# From the packaged candidate checkout, JDK 21 and WebKit configured as for run-browser.ps1.
$ErrorActionPreference = 'Stop'
$source = Get-Content -Raw -Encoding UTF8 docs/qa/AUDIT/run-browser.ps1
if (!$source.Contains("'--app.audit.clearance-source-enabled=true', ")) { throw 'Fixture flag changed' }
$source = $source.Replace("'--app.audit.clearance-source-enabled=true', ", '')
$source = $source.Replace('docs/qa/AUDIT/browser.cjs','docs/qa/AUDIT/final-review/default-gate.cjs')
& ([scriptblock]::Create($source))
