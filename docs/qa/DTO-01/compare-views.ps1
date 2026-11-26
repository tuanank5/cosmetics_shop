$ErrorActionPreference = 'Stop'

function Read-Snapshot($path) {
    $rows = @{}
    foreach ($line in Get-Content -LiteralPath $path) {
        if ($line -match '^\[DTO-01\] (\S+) selects=(\d+) fields=\[(.+)\] html=([0-9a-f]{64})$') {
            $rows[$Matches[1]] = @{ Queries = [int]$Matches[2]; Fields = $Matches[3]; Hash = $Matches[4] }
        }
    }
    return $rows
}

$before = Read-Snapshot (Join-Path $PSScriptRoot 'query-before.txt')
$after = Read-Snapshot (Join-Path $PSScriptRoot 'query-after.txt')
if ($before.Count -ne 13 -or $after.Count -ne 13) { throw 'Incomplete before/after snapshot set' }

$lines = foreach ($name in ($before.Keys | Sort-Object)) {
    $old = $before[$name]
    $new = $after[$name]
    if (!$new -or $old.Hash -ne $new.Hash -or $old.Queries -lt $new.Queries) {
        throw "HTML changed or query count increased for $name"
    }
    "$name HTML unchanged | SELECT $($old.Queries) -> $($new.Queries) | $($old.Fields) -> $($new.Fields)"
}
foreach ($view in @('address', 'checkout', 'catalog', 'pdp', 'sku-admin')) {
    if ($after["$view-1"].Queries -ne $after["$view-7"].Queries) { throw "Query count scales by row for $view" }
}
$lines | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'comparison.txt') -Encoding utf8
$lines
