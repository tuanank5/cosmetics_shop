[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('test', 'package', 'run')]
    [string]$Task
)

$ErrorActionPreference = 'Stop'
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $maven) {
    throw 'Install Maven 3.9+ and add its bin directory to PATH.'
}

if ($Task -eq 'run' -and [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw 'Set DB_PASSWORD before running the dev profile. See docs/deployment/01-local-db-and-flyway.md.'
}

$mavenArgs = switch ($Task) {
    'test'    { '-B', 'test' }
    'package' { '-B', '-DskipTests', 'package' }
    'run'     { 'spring-boot:run', '-Dspring-boot.run.profiles=dev' }
}

Push-Location (Join-Path $PSScriptRoot '..')
try {
    & $maven.Source @mavenArgs
    $result = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $result
