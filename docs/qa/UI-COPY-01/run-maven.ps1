$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$build = Join-Path $root 'target/ui-copy-01'
Push-Location $root
try {
    $null = New-Item -ItemType Directory -Path $build -Force
    Copy-Item -LiteralPath "$root/src", "$root/pom.xml" -Destination $build -Recurse -Force
    $env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.13.10002-hotspot'
    $mavenArgs = @('-o', '-B', '-ntp', '-f', "$build/pom.xml")
    $steps = [ordered]@{ test = @('test'); package = @('-DskipTests', 'package') }
    foreach ($step in $steps.Keys) {
        $log = "$build/$step.raw.log"
        $options = $steps[$step]
        $ErrorActionPreference = 'Continue'
        & mvn.cmd @mavenArgs @options *> $log
        $code = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        (Select-String -LiteralPath $log -Pattern '^\[(INFO|ERROR)\] (BUILD |Tests run:.*Skipped: \d+$)').Line |
            Set-Content -Encoding UTF8 "$PSScriptRoot/$step.txt"
        if ($code) { throw "Maven $step failed. See $log." }
    }
} finally {
    Pop-Location
}
