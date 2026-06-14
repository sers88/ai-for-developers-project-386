#requires -Version 7
[CmdletBinding()]
param(
    [ValidateSet("frontend", "backend", "all")]
    [string]$Target = "all",

    [ValidateSet("dev", "preview")]
    [string]$FrontendMode = "dev"
)
$ErrorActionPreference = "Stop"

$upScript   = Join-Path $PSScriptRoot "up.ps1"
$downScript = Join-Path $PSScriptRoot "down.ps1"

function Get-PortOwner($port) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { return $c.OwningProcess } else { return $null }
}
function Stop-Port($port) {
    $owner = Get-PortOwner $port
    if ($owner) {
        "stopping port $port (PID=$owner)"
        Stop-Process -Id $owner -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

if ($Target -eq "all") {
    & $downScript
    & $upScript -FrontendMode $FrontendMode
    return
}

if ($Target -eq "frontend") { Stop-Port 3000 }
elseif ($Target -eq "backend") { Stop-Port 8080 }

# up.ps1 is idempotent: it skips already-healthy services and only relaunches
# the one we just stopped.
& $upScript -FrontendMode $FrontendMode
