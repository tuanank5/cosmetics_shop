function Import-BeautyShopLocalEnv([string] $Path, [Collections.Generic.List[string]] $Imported) {
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { return }

    $values = @{}
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $lineNumber++
        if ($line -match '^\s*(?:#.*)?$') { continue }
        if ($line -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            throw "Invalid .env assignment on line $lineNumber."
        }
        $name = $Matches[1]
        $value = $Matches[2].Trim()
        if ($value -match "^'([^']*)'\s*(?:#.*)?$" -or $value -match '^"([^"]*)"\s*(?:#.*)?$') {
            $value = $Matches[1]
        } elseif ($value -match '^[\x22\x27]') {
            throw "Invalid quoted .env value on line $lineNumber."
        } else {
            $value = ($value -replace '(?:^|\s+)#.*$', '').TrimEnd()
        }
        $values[$name] = $value
    }

    foreach ($name in $values.Keys) {
        if ($null -eq [Environment]::GetEnvironmentVariable($name)) {
            [Environment]::SetEnvironmentVariable($name, $values[$name])
            $Imported.Add($name)
        }
    }
}

function Clear-BeautyShopLocalEnv([Collections.Generic.List[string]] $Imported) {
    foreach ($name in $Imported) {
        [Environment]::SetEnvironmentVariable($name, $null)
    }
}

function Get-BeautyShopLocalDatabaseUrl {
    if ($env:DB_URL) { return $env:DB_URL }
    $port = if ($env:DB_PORT) { $env:DB_PORT } else { '3306' }
    $database = if ($env:DB_NAME) { $env:DB_NAME } else { 'beautyshop' }
    if ($port -notmatch '^[0-9]{1,5}$' -or [int]$port -lt 1 -or [int]$port -gt 65535 -or
        $database -cnotmatch '^[a-zA-Z0-9_]+$') {
        throw 'Set a valid DB_PORT and DB_NAME, or an explicit DB_URL.'
    }
    return "jdbc:mariadb://127.0.0.1:${port}/$database"
}

function Get-BeautyShopDemoDatabase([string] $Database, [string] $User, [int] $Port) {
    $url = Get-BeautyShopLocalDatabaseUrl
    if ($url -notmatch '^jdbc:mariadb://(?:localhost|127\.0\.0\.1)(?::([0-9]{1,5}))?/([a-zA-Z0-9_]+)(?:\?[^\r\n]*)?$') {
        throw 'Demo seed requires a loopback MariaDB URL with an explicit schema.'
    }
    if (!$Database) { $Database = $Matches[2] }
    if (!$Port) { $Port = if ($Matches[1]) { [int]$Matches[1] } else { 3306 } }
    if (!$User) { $User = if ($env:DB_USER) { $env:DB_USER } else { 'beautyshop' } }
    if ($Database -cnotmatch '^beautyshop_demo(?:_[a-z0-9]+)?$') {
        throw 'Demo seed requires DB_NAME=beautyshop_demo or an explicitly selected beautyshop_demo_* schema.'
    }
    if ($Port -lt 1 -or $Port -gt 65535) { throw 'Invalid MariaDB port.' }
    return @{ Database = $Database; User = $User; Port = $Port }
}
