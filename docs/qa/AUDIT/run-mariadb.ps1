param([Parameter(Mandatory)][string]$Checkout, [Parameter(Mandatory)][string]$MavenRepo,
      [Parameter(Mandatory)][string]$EvidenceDirectory)
$runner = Get-Content -Encoding UTF8 -Raw "$PSScriptRoot/../QUALITY/final-review/run-mariadb.ps1"
if (!$runner.Contains('-Dtest=CheckoutIntegrityTest,')) { throw 'Integration suite changed' }
& ([scriptblock]::Create($runner.Replace('-Dtest=CheckoutIntegrityTest,',
    '-Dtest=Audit_MariaDb_Test,DanhMucSanPhamThuongHieu_MariaDb_Test,CheckoutIntegrityTest,'))) @PSBoundParameters
