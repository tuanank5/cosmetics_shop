param(
    [Parameter(Mandatory)][string]$MariaDbBin,
    [Parameter(Mandatory)][string]$RedisBin,
    [Parameter(Mandatory)][string]$Mailpit,
    [string]$TestClasses = 'XacThuc_MariaDb_Test,DonHangCOD_MariaDb_Test',
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$qa = if ($OutputPath) { (New-Item -ItemType Directory -Path (Join-Path $repo $OutputPath) -Force).FullName } else { $PSScriptRoot }
$runtime = (New-Item -ItemType Directory -Path (Join-Path $repo ('target/p04-otp/run-' + [guid]::NewGuid()))).FullName
$MariaDbBin = (Resolve-Path $MariaDbBin).Path
$RedisBin = (Resolve-Path $RedisBin).Path
$Mailpit = (Resolve-Path $Mailpit).Path
$jar = (Resolve-Path (Join-Path $repo 'target/beautyshop-0.0.1-SNAPSHOT.jar')).Path
$processes = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()

function Get-FreePort {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    try {
        $listener.Start()
        return $listener.LocalEndpoint.Port
    } finally {
        $listener.Stop()
    }
}

function Start-Helper($name, $program, $argsList) {
    $processes.Add((Start-Process -FilePath $program -ArgumentList $argsList -WorkingDirectory $runtime -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $qa "$name-stdout.log") -RedirectStandardError (Join-Path $qa "$name-stderr.log")))
}

function Wait-Http($url) {
    for ($i = 0; $i -lt 90; $i++) {
        if ($processes[$processes.Count - 1].HasExited) { throw "Helper exited while waiting for $url" }
        try {
            if ((Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 2).StatusCode -eq 200) { return }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    throw "Timed out while waiting for $url"
}

try {
    Set-Location $repo
    $dbPort = Get-FreePort
    $datadir = Join-Path $runtime 'mariadb'
    $env:DB_PASSWORD = [guid]::NewGuid().ToString('N')
    $env:DB_USER = 'root'
    $env:MYSQL_PWD = $env:DB_PASSWORD
    $env:DB_URL = "jdbc:mariadb://127.0.0.1:$dbPort/beautyshop_p04otp"
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:REVIEWS_STORE = 'mariadb'
    $env:P01_MARIADB_TEST = 'true'
    $env:TEMP = $runtime
    $env:TMP = $runtime

    & (Join-Path $MariaDbBin 'mariadb-install-db.exe') "--datadir=$datadir" "--port=$dbPort" "--password=$env:DB_PASSWORD" > (Join-Path $qa 'mariadb-bootstrap.log')
    if ($LASTEXITCODE -ne 0) { throw 'MariaDB bootstrap failed' }
    Start-Helper 'mariadb' (Join-Path $MariaDbBin 'mariadbd.exe') @("--defaults-file=`"$datadir/my.ini`"", '--bind-address=127.0.0.1', '--console')
    $db = Join-Path $MariaDbBin 'mariadb.exe'
    for ($i = 0; $i -lt 60; $i++) {
        $probe = [System.Net.Sockets.TcpClient]::new()
        try {
            $probe.Connect('127.0.0.1', $dbPort)
            break
        } catch {
            if ($i -eq 59) { throw 'MariaDB did not open its port' }
            Start-Sleep -Milliseconds 500
        } finally {
            $probe.Dispose()
        }
    }
    & $db --no-defaults --protocol=tcp --host=127.0.0.1 "--port=$dbPort" --user=root '--execute=CREATE DATABASE beautyshop_p04otp'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot create test database' }
    Remove-Item Env:MYSQL_PWD

    $mavenRepo = Join-Path $env:USERPROFILE '.m2/repository'
    mvn.cmd -B -ntp "-Dmaven.repo.local=$mavenRepo" "-Dtest=$TestClasses" test > (Join-Path $qa 'mvn-mariadb.log')
    if ($LASTEXITCODE -ne 0) { throw 'MariaDB test failed' }

    $env:OTP_REDIS_HOST = '127.0.0.1'
    $env:OTP_REDIS_PORT = Get-FreePort
    $env:P04_QA_REDIS_CLI = Join-Path $RedisBin 'redis-cli.exe'
    @('bind 127.0.0.1', "port $env:OTP_REDIS_PORT", 'save ""', 'appendonly no') |
        Set-Content -LiteralPath (Join-Path $runtime 'qa-redis.conf') -Encoding ASCII
    Start-Helper 'redis' (Join-Path $RedisBin 'redis-server.exe') @('qa-redis.conf')

    $env:OTP_MAIL_HOST = '127.0.0.1'
    $env:OTP_MAIL_PORT = Get-FreePort
    $env:OTP_MAIL_FROM = 'no-reply@beautyshop.test'
    $mailpitPort = Get-FreePort
    $env:P04_QA_MAILPIT = "http://127.0.0.1:$mailpitPort"
    Start-Helper 'mailpit' $Mailpit @('--listen', "127.0.0.1:$mailpitPort", '--smtp', "127.0.0.1:$env:OTP_MAIL_PORT", '--database', 'mailpit.db')
    Wait-Http "$env:P04_QA_MAILPIT/api/v1/messages"

    $env:SERVER_ADDRESS = '127.0.0.1'
    $env:PORT = Get-FreePort
    $env:P04_QA_APP = "http://127.0.0.1:$env:PORT"
    Start-Helper 'app' (Get-Command java.exe).Source @("-Djava.io.tmpdir=`"$runtime`"", '-jar', "`"$jar`"")
    Wait-Http "$env:P04_QA_APP/register"
    node (Join-Path $PSScriptRoot 'kiem-tra-otp.mjs') | Tee-Object -FilePath (Join-Path $qa 'otp-http.log')
    if ($LASTEXITCODE -ne 0) { throw 'OTP HTTP QA failed' }
} finally {
    foreach ($process in $processes) {
        if (!$process.HasExited) { Stop-Process -Id $process.Id }
    }
}
