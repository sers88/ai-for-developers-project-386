#requires -Version 7
[CmdletBinding()]
param()
$ErrorActionPreference = "Stop"

$repoRoot    = (Resolve-Path (Join-Path $PSScriptRoot "../../../..")).Path
$composeFile = Join-Path $repoRoot "docker-compose.yml"

function Get-PortOwner($port) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { return $c.OwningProcess } else { return $null }
}
function Stop-Port($port, $label) {
    $owner = Get-PortOwner $port
    if ($owner) {
        "stopping $label (PID=$owner)"
        Stop-Process -Id $owner -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    } else {
        "$label : not running"
    }
}

Stop-Port 3000 "frontend"
Stop-Port 8080 "backend"

"stopping postgres container..."
docker compose -f $composeFile stop postgres 2>&1 | Out-Null

"== down complete =="
