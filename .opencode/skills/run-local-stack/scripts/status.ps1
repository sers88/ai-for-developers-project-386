#requires -Version 7
[CmdletBinding()]
param()

function Probe($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 3 -UseBasicParsing
        if ($r.StatusCode -eq 200) { return "UP" } else { return "HTTP $($r.StatusCode)" }
    } catch {
        return "DOWN"
    }
}
function Owner($port) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { return "$($c.OwningProcess)" } else { return "-" }
}

$pgState = docker inspect --format "{{.State.Health.Status}}" postgres 2>$null
if (-not $pgState) { $pgState = "not-created" }

"{0,-10} {1,-6} {2,-8} {3}" -f "ROLE", "PORT", "PID", "STATUS"
"{0,-10} {1,-6} {2,-8} {3}" -f "frontend", "3000", (Owner 3000), (Probe "http://localhost:3000")
"{0,-10} {1,-6} {2,-8} {3}" -f "backend", "8080", (Owner 8080), (Probe "http://localhost:8080/actuator/health")
"{0,-10} {1,-6} {2,-8} {3}" -f "postgres", "5432", "docker", $pgState
