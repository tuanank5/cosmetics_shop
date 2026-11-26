[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('test', 'package', 'run', 'migrate', 'demo')]
    [string]$Task,
    [switch]$Offline,
    [string]$Client
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-env.ps1')
$maven = Get-Command mvn.cmd,mvn -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $maven) {
    throw 'Install Maven 3.9+ and add its bin directory to PATH.'
}

$mavenOptions = @('-B')
if ($Offline) { $mavenOptions += '-o' }
$imported = [Collections.Generic.List[string]]::new()

Push-Location (Join-Path $PSScriptRoot '..')
try {
    if ($Task -in @('run', 'migrate', 'demo')) {
        Import-BeautyShopLocalEnv (Join-Path (Get-Location) '.env') $imported
        if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
            throw 'Set DB_PASSWORD in .env or in the current shell. See docs/deployment/01-local-db-and-flyway.md.'
        }
        if (!$env:DB_URL) {
            $env:DB_URL = Get-BeautyShopLocalDatabaseUrl
            $imported.Add('DB_URL')
        }
    }

    if ($Task -eq 'demo') {
        Get-BeautyShopDemoDatabase | Out-Null
        if (!$Client) {
            $maria = Get-Command mariadb.exe,mariadb -ErrorAction SilentlyContinue | Select-Object -First 1
            if (!$maria) { throw 'Add the MariaDB client to PATH or pass -Client.' }
            $Client = $maria.Source
        } elseif (!(Get-Command $Client -CommandType Application -ErrorAction SilentlyContinue)) {
            throw 'The MariaDB client specified by -Client was not found.'
        }
    }
    if ($Task -in @('migrate', 'demo')) {
        & $maven.Source @mavenOptions spring-boot:run '-Dspring-boot.run.arguments=--migrate-only'
        $result = $LASTEXITCODE
        if ($result -ne 0 -or $Task -eq 'migrate') { exit $result }
        & (Join-Path $PSScriptRoot 'seed-demo.ps1') -WithOrders -Client $Client
    }

    $mavenArgs = switch ($Task) {
        'test'    { 'test' }
        'package' { '-DskipTests', 'package' }
        'demo'    { 'spring-boot:run', '-Dspring-boot.run.profiles=dev', '-Dspring-boot.run.arguments=--server.address=127.0.0.1' }
        default   { 'spring-boot:run', '-Dspring-boot.run.profiles=dev' }
    }
    & $maven.Source @mavenOptions @mavenArgs
    $result = $LASTEXITCODE
} finally {
    Clear-BeautyShopLocalEnv $imported
    Pop-Location
}
exit $result
