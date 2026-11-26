[CmdletBinding()]
param(
    [string] $Database,
    [string] $User,
    [ValidateRange(1, 65535)]
    [int] $Port,
    [string] $Client,
    [switch] $WithOrders,
    [ValidateRange(80, 150)]
    [int] $OrderCount = 120,
    [ValidateRange(5, 8)]
    [int] $CustomerCount = 6,
    [int] $Seed = 20261123
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-env.ps1')
$imported = [Collections.Generic.List[string]]::new()
try {
    Import-BeautyShopLocalEnv (Join-Path $PSScriptRoot '../.env') $imported
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
        throw 'Set DB_PASSWORD in .env or in the current shell.'
    }
    $target = Get-BeautyShopDemoDatabase $Database $User $Port
    if (!$Client) {
        $maria = Get-Command mariadb.exe,mariadb -ErrorAction SilentlyContinue | Select-Object -First 1
        if (!$maria) { throw 'Add the MariaDB client to PATH or pass -Client.' }
        $Client = $maria.Source
    }
    & (Join-Path $PSScriptRoot 'internal/seed-demo-catalog.ps1') @target -Client $Client
    if ($WithOrders) {
        & (Join-Path $PSScriptRoot 'seed-demo-orders.ps1') @target -Client $Client -OrderCount $OrderCount -CustomerCount $CustomerCount -Seed $Seed
    }
} finally {
    Clear-BeautyShopLocalEnv $imported
}
