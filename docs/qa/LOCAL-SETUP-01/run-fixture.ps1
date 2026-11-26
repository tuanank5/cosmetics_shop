param(
    [string] $MariaDbBin = 'C:/Program Files/MariaDB 12.2/bin',
    [switch] $DemoOnly
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$scratch = Join-Path $root ('target/local-setup-' + [Guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $scratch, "$scratch/docs/data"
Copy-Item -LiteralPath "$root/src", "$root/scripts" -Destination $scratch -Recurse
Copy-Item -LiteralPath "$root/pom.xml" -Destination $scratch
Copy-Item -LiteralPath "$root/docs/data/product-research.csv" -Destination "$scratch/docs/data"
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot'
$java = "$env:JAVA_HOME/bin/java.exe"
$sqlPort = 33391
$webPort = 18111
foreach ($port in $sqlPort, $webPort) {
    $probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
    $probe.Start()
    $probe.Stop()
}

$password = [Guid]::NewGuid().ToString('N') + ' #$'
$envFile = "$scratch/.env"
$schema = 'beautyshop_demo_setup'
$jdbc = "jdbc:mariadb://127.0.0.1:$sqlPort/$schema"
$db = $null
$web = $null
$demoTask = $null
$qaEvidence = if ($DemoOnly) { Join-Path $PSScriptRoot 'demo-task.txt' } else { Join-Path $PSScriptRoot 'results.txt' }
$envNames = @('DB_URL', 'DB_NAME', 'DB_PORT', 'DB_USER', 'DB_PASSWORD', 'MYSQL_PWD', 'LOCAL_BOOTSTRAP_MIGRATION_FIXTURE',
    'MONGODB_URI', 'MONGODB_TEST_URI', 'P01_MARIADB_TEST', 'P23_MONGO_TEST', 'AUTH_02_MIGRATION_FIXTURE',
    'ARCH_01_BSON_PHASE', 'P21_DEMO_REPORT', 'PORT')
$previousEnv = @{}
foreach ($name in $envNames) {
    $previousEnv[$name] = [Environment]::GetEnvironmentVariable($name)
    [Environment]::SetEnvironmentVariable($name, $null)
}

function Invoke-Checked([string] $Name, [string] $Command, [string[]] $Arguments) {
    $log = Join-Path $scratch "$Name.raw.log"
    $ErrorActionPreference = 'Continue'
    & $Command @Arguments *> $log
    $code = $LASTEXITCODE
    if ($code) { throw "$Name failed with exit code $code. See $log." }
    $summary = (Select-String -LiteralPath $log -Pattern '^\[INFO\] BUILD SUCCESS|^\[INFO\] Tests run:.*Skipped: \d+$|^Demo catalog imported:|^Demo orders already exist|^Demo wishlists:').Line
    @("$Name PASS") + @($summary) | Add-Content -Encoding UTF8 $qaEvidence
}

function Wait-SeededProduct([Diagnostics.Process] $Process, [int] $Attempts) {
    for ($attempt = 0; $attempt -lt $Attempts -and !$Process.HasExited; $attempt++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$webPort/products/demo-r026" -TimeoutSec 2
            if ($response.StatusCode -eq 200) { return }
        } catch { Start-Sleep -Milliseconds 500 }
    }
    throw "Dev startup or seeded product smoke failed. See $scratch."
}

try {
    Set-Content -Encoding UTF8 $qaEvidence "JDK 21 / MariaDB fixture / localhost $sqlPort"
    $install = "$MariaDbBin/mariadb-install-db.exe"
    # Use an initial simple password for MariaDB initialization, then rotate it in the fixture.
    $initial = [Guid]::NewGuid().ToString('N')
    & $install "--datadir=$scratch/db" "--password=$initial" "--port=$sqlPort" --silent | Out-Null
    if ($LASTEXITCODE) { throw 'MariaDB fixture initialization failed.' }
    $db = Start-Process "$MariaDbBin/mariadbd.exe" -WindowStyle Hidden -PassThru -ArgumentList @(
        ('--defaults-file="' + (Join-Path $scratch 'db/my.ini') + '"'), '--bind-address=127.0.0.1', '--console'
    ) -RedirectStandardOutput "$scratch/db.log" -RedirectStandardError "$scratch/db-error.log"
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        try {
            $socket = [Net.Sockets.TcpClient]::new('127.0.0.1', $sqlPort)
            $socket.Dispose()
            break
        } catch { Start-Sleep -Milliseconds 250 }
    }
    $client = "$MariaDbBin/mariadb.exe"
    $sql = @('--no-defaults', '--skip-ssl', '--protocol=tcp', '--host=127.0.0.1', "--port=$sqlPort", '--user=root')
    $env:MYSQL_PWD = $initial
    $create = @($schema) + @(1..6 | ForEach-Object { "${schema}_v$_" }) + @("${schema}_unmanaged", "${schema}_drift")
    $statements = $create | ForEach-Object { "CREATE DATABASE $_ CHARACTER SET utf8mb4;" }
    ($statements -join "`n") | & $client @sql
    if ($LASTEXITCODE) { throw 'Creating fixture schemas failed.' }
    $pdo = @('CREATE USER ''fixture''@''127.0.0.1'' IDENTIFIED BY ''' + $password + ''';') +
        @($create | ForEach-Object { "GRANT ALL PRIVILEGES ON $_.* TO 'fixture'@'127.0.0.1';" })
    ($pdo -join "`n") | & $client @sql
    if ($LASTEXITCODE) { throw 'Creating the fixture user failed.' }
    $env:MYSQL_PWD = $null
    @("DB_PORT=$sqlPort", "DB_NAME=$schema", "DB_USER=fixture", "DB_PASSWORD='$password' # fixture") |
        Set-Content -LiteralPath $envFile -Encoding UTF8

    $launcher = "$scratch/scripts/p00.ps1"
    $seed = "$scratch/scripts/seed-demo.ps1"
    $flags = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File')
    if ($DemoOnly) {
        $env:PORT = "$webPort"
        $env:DB_PORT = "$sqlPort"
        $env:MONGODB_URI = 'mongodb://127.0.0.1:1/beautyshop_setup?serverSelectionTimeoutMS=100'
        @('DB_PORT=1', "DB_NAME=$schema", 'DB_USER=fixture', "DB_PASSWORD='$password' # fixture") |
            Set-Content -LiteralPath $envFile -Encoding UTF8
        $demoTask = Start-Process powershell.exe -WindowStyle Hidden -PassThru -ArgumentList (
            $flags + @('"' + $launcher + '"', '-Task', 'demo', '-Offline', '-Client', '"' + $client + '"')
        ) -RedirectStandardOutput "$scratch/demo.log" -RedirectStandardError "$scratch/demo-error.log"
        Wait-SeededProduct $demoTask 200
        'p00 -Task demo PASS: fresh schema, seed, dev startup and HTTP 200. Shell DB_PORT wins over .env.' |
            Add-Content -Encoding UTF8 $qaEvidence
        return
    }
    Invoke-Checked 'fresh-migrate-from-dotenv' 'powershell.exe' ($flags + @($launcher, '-Task', 'migrate', '-Offline'))
    Invoke-Checked 'second-migrate-from-dotenv' 'powershell.exe' ($flags + @($launcher, '-Task', 'migrate', '-Offline'))
    Invoke-Checked 'seed-catalog-orders-wishlist' 'powershell.exe' ($flags + @($seed, '-WithOrders', '-Client', $client))
    $env:MYSQL_PWD = $password
    $seedSql = @('--no-defaults', '--skip-ssl', '--protocol=tcp', '--host=127.0.0.1', "--port=$sqlPort", '--user=fixture', '-N', '-B', $schema)
    $countsQuery = "SELECT COUNT(*) FROM san_pham; SELECT COUNT(*) FROM bien_the_san_pham; SELECT COUNT(*) FROM don_hang; SELECT COUNT(*) FROM san_pham_yeu_thich; SELECT SUM(so_luong_trong_kho) FROM bien_the_san_pham; SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success;"
    $before = @(& $client @seedSql -e $countsQuery)
    if ($LASTEXITCODE -or $before.Count -ne 6 -or $before[0] -ne '31' -or $before[1] -ne '32' -or
        [int]$before[2] -lt 80 -or $before[3] -ne '30' -or $before[5] -ne '7') {
        throw 'Unexpected demo seed counts.'
    }
    Invoke-Checked 'repeat-seed' 'powershell.exe' ($flags + @($seed, '-WithOrders', '-Client', $client))
    $after = @(& $client @seedSql -e $countsQuery)
    if ($LASTEXITCODE -or ($before -join ',') -ne ($after -join ',')) { throw 'Repeated seed changed data or inventory.' }
    "Seed after repeat: $($after[0]) products, $($after[1]) SKUs, $($after[2]) orders, $($after[3]) wishlists, $($after[4]) stock, Flyway V$($after[5])." |
        Add-Content -Encoding UTF8 $qaEvidence

    $maven = (Get-Command mvn.cmd).Source
    $mavenArgs = @('-o', '-B', '-ntp', '-f', "$scratch/pom.xml")
    Invoke-Checked 'maven-test' $maven ($mavenArgs + @('test'))
    $env:DB_URL = $jdbc
    $env:DB_USER = 'fixture'
    $env:DB_PASSWORD = $password
    $env:LOCAL_BOOTSTRAP_MIGRATION_FIXTURE = 'true'
    Invoke-Checked 'upgrade-v1-v6-and-reject-drift' $maven (
        $mavenArgs + @('-Dtest=LocalDatabaseMigration_MariaDb_Test', 'test'))
    Invoke-Checked 'maven-package' $maven ($mavenArgs + @('-DskipTests', 'package'))

    $env:DB_URL = $null
    $env:DB_PORT = "$sqlPort"
    $env:DB_NAME = $schema
    $env:MONGODB_URI = 'mongodb://127.0.0.1:1/beautyshop_setup?serverSelectionTimeoutMS=100'
    $web = Start-Process $java -WindowStyle Hidden -PassThru -ArgumentList @(
        '-jar', ('"' + (Join-Path $scratch 'target/beautyshop-0.0.1-SNAPSHOT.jar') + '"'),
        '--spring.profiles.active=dev', '--server.address=127.0.0.1', "--server.port=$webPort"
    ) -RedirectStandardOutput "$scratch/app.log" -RedirectStandardError "$scratch/app-error.log"
    Wait-SeededProduct $web 45
    'Dev startup PASS: Flyway validate, JPA validate and seeded PDP returned HTTP 200 using DB_PORT/DB_NAME.' |
        Add-Content -Encoding UTF8 $qaEvidence
} finally {
    if ($demoTask -and !$demoTask.HasExited) { & taskkill.exe /PID $demoTask.Id /T /F | Out-Null }
    if ($web -and !$web.HasExited) { Stop-Process -Id $web.Id }
    if ($db -and !$db.HasExited) { Stop-Process -Id $db.Id }
    if (Test-Path -LiteralPath $envFile) { Remove-Item -LiteralPath $envFile }
    foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $previousEnv[$name]) }
}
