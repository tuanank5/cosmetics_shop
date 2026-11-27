param([ValidateSet('all','test','package')][string]$Phase = 'all')

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$work = (New-Item -ItemType Directory -Force (Join-Path $root 'target/ui-redesign-02')).FullName
$build = (New-Item -ItemType Directory -Force "$work/current").FullName
$cache = "$work/maven"

Copy-Item -LiteralPath "$root/pom.xml","$root/src" -Destination $build -Recurse -Force
if (!(Test-Path -LiteralPath $cache)) {
    $priorCache = "$root/target/ui-redesign-01/maven"
    if (!(Test-Path -LiteralPath $priorCache)) { throw 'Nạp dependency vào target/ui-redesign-02/maven trước khi chạy offline.' }
    robocopy.exe $priorCache $cache /E /MT:12 /NFL /NDL /NJH /NJS /NP
    if ($LASTEXITCODE -gt 7) { throw 'Maven cache copy failed.' }
}
Get-ChildItem Env: | Where-Object Name -Match '^(SPRING_|DB_|MONGO|REDIS|MAIL|MOMO|OAUTH|GOOGLE_|GITHUB_|AI_|OLLAMA|APP_UPLOADS|AUTH_PUBLIC_URL|P01_|P23_|AUTH_02_|LOCAL_BOOTSTRAP_|P21_|ARCH_01_|DATA_01_)' |
    ForEach-Object { [Environment]::SetEnvironmentVariable($_.Name, $null, 'Process') }
$env:JAVA_HOME = Split-Path (Split-Path (Get-Command java.exe).Source)
$env:DB_URL = 'jdbc:mariadb://127.0.0.1:1/ui_redesign_02'
$env:MONGODB_URI = 'mongodb://127.0.0.1:1/ui_redesign_02'
$env:REDIS_PORT = '1'
$env:MAIL_HOST = '127.0.0.1'
$env:MAIL_PORT = '1'
$env:APP_UPLOADS_SKU_DIR = "$work/uploads"

function Invoke-Build($name, [string[]]$goals) {
    $log = "$work/maven-$name.log"
    $summary = Join-Path $PSScriptRoot "maven-$name.txt"
    $ErrorActionPreference = 'Continue'
    & mvn.cmd -B -o -ntp "-Dmaven.repo.local=$cache" -f "$build/pom.xml" @goals *> $log
    $code = $LASTEXITCODE
    Select-String -Path $log -Pattern '^\[(?:INFO|ERROR)\] (Tests run: |BUILD (?:SUCCESS|FAILURE))' |
        ForEach-Object Line | Set-Content -LiteralPath $summary -Encoding utf8
    Get-Content -LiteralPath $summary | Select-Object -Last 2
    if ($code -ne 0) { exit $code }
}

if ($Phase -in 'all','test') { Invoke-Build 'test' @('test') }
if ($Phase -in 'all','package') { Invoke-Build 'package' @('-DskipTests','package') }
