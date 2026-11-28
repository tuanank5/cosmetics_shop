param([Parameter(Mandatory)][string]$Checkout,[Parameter(Mandatory)][string]$MavenRepo,
      [Parameter(Mandatory)][string]$EvidenceDirectory)
$runner = Get-Content -Raw -Encoding UTF8 "$PSScriptRoot/../../QUALITY/final-review/run-mariadb.ps1"
$schemas = @('beautyshop_demo_setup', 'beautyshop_demo_setup_drift', 'beautyshop_demo_setup_unmanaged')
$schemas += 1..8 | ForEach-Object { "beautyshop_demo_setup_v$_" }
$create = ($schemas | ForEach-Object { "CREATE DATABASE $_ CHARACTER SET utf8mb4;" }) -join ' '
$runner = $runner -replace "CREATE DATABASE auth_02_test[^']+", $create
$runner = $runner.Replace('33472','33475').Replace('/auth_02_test','/beautyshop_demo_setup')
$runner = $runner -replace '-Dtest=CheckoutIntegrityTest,[^'']+', '-Dtest=LocalDatabaseMigration_MariaDb_Test'
$runner = $runner.Replace("'integration.txt'","'bootstrap.txt'")
$env:LOCAL_BOOTSTRAP_MIGRATION_FIXTURE = 'true'
& ([scriptblock]::Create($runner)) @PSBoundParameters
