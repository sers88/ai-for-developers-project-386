#requires -Version 7
[CmdletBinding()]
param(
    [ValidateSet("dev", "preview")]
    [string]$FrontendMode = "dev"
)
$ErrorActionPreference = "Stop"

$repoRoot    = (Resolve-Path (Join-Path $PSScriptRoot "../../../..")).Path
$logDir      = Join-Path $env:LOCALAPPDATA "aifordev-run\logs"
$envFile     = Join-Path $repoRoot ".env"
$composeFile = Join-Path $repoRoot "docker-compose.yml"
$backendDir  = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (-not (Test-Path $envFile)) { throw ".env not found at $envFile" }

# ---------------- helpers ----------------
function Get-EnvVar($key) {
    $line = Get-Content $envFile | Where-Object { $_ -match "^$key=" } | Select-Object -First 1
    if (-not $line) { throw "Missing '$key' in .env" }
    return ($line -replace "^$key=", "")
}
function Get-PortOwner($port) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { return $c.OwningProcess } else { return $null }
}
function Stop-Port($port) {
    $owner = Get-PortOwner $port
    if ($owner) { Stop-Process -Id $owner -Force -ErrorAction SilentlyContinue; Start-Sleep -Seconds 2 }
}
function Wait-HttpStatus($url, $timeoutSec) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $url -TimeoutSec 4 -UseBasicParsing
            if ($r.StatusCode -eq 200) { return $true }
        } catch {}
        Start-Sleep -Seconds 2
    }
    return $false
}
function Wait-DockerDaemon($timeoutSec) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $v = docker info --format "{{.ServerVersion}}" 2>$null
        if ($LASTEXITCODE -eq 0 -and $v) { return $true }
        Start-Sleep -Seconds 4
    }
    return $false
}
function Wait-ComposeHealthy($service, $timeoutSec) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $s = docker inspect --format "{{.State.Health.Status}}" $service 2>$null
        if ($s -eq "healthy") { return $true }
        Start-Sleep -Seconds 3
    }
    return $false
}

# ---------------- 1. postgres ----------------
"== postgres =="
$pgPort = Get-EnvVar "POSTGRES_PORT"
$pgDb   = Get-EnvVar "POSTGRES_DB"
$pgUser = Get-EnvVar "POSTGRES_USER"

$h = docker inspect --format "{{.State.Health.Status}}" postgres 2>$null
if ($h -eq "healthy") {
    "postgres already healthy - skip"
} else {
    if (-not (Wait-DockerDaemon 90)) { throw "Docker daemon not ready after 90s - start Docker Desktop" }
    docker compose -f $composeFile up -d postgres 2>&1 | Out-Null
    if (-not (Wait-ComposeHealthy "postgres" 60)) { throw "postgres not healthy after 60s" }
    "postgres up (db=$pgDb port=$pgPort)"
}

# ---------------- 2. backend ----------------
"== backend =="
$beHealth = "http://localhost:8080/actuator/health"
$beOut = Join-Path $logDir "backend.out.log"
$beErr = Join-Path $logDir "backend.err.log"
if (Wait-HttpStatus $beHealth 3) {
    "backend already UP - skip"
} else {
    if (Get-PortOwner 8080) { "port 8080 busy (not healthy) - killing"; Stop-Port 8080 }

    $env:SPRING_DATASOURCE_URL        = "jdbc:postgresql://localhost:$pgPort/$pgDb"
    $env:SPRING_DATASOURCE_USERNAME   = $pgUser
    $env:SPRING_DATASOURCE_PASSWORD   = Get-EnvVar "POSTGRES_PASSWORD"
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = "validate"
    $env:JWT_SECRET                   = Get-EnvVar "JWT_SECRET"

    $p = Start-Process -FilePath (Join-Path $backendDir "gradlew.bat") `
        -ArgumentList "bootRun", "--console=plain" `
        -WorkingDirectory $backendDir `
        -RedirectStandardOutput $beOut -RedirectStandardError $beErr `
        -WindowStyle Hidden -PassThru
    "backend launching (PID=$($p.Id))..."
    if (-not (Wait-HttpStatus $beHealth 150)) {
        if ($p.HasExited) { "backend process EXITED unexpectedly" }
        "--- backend.out tail ---"; Get-Content $beOut -Tail 20 -ErrorAction SilentlyContinue
        "--- backend.err tail ---"; Get-Content $beErr -Tail 20 -ErrorAction SilentlyContinue
        throw "backend not UP at $beHealth after 150s"
    }
    "backend UP"
}

# ---------------- 3. frontend ----------------
"== frontend ($FrontendMode) =="
$feHealth = "http://localhost:3000"
$feOut = Join-Path $logDir "frontend.out.log"
$feErr = Join-Path $logDir "frontend.err.log"
if (Wait-HttpStatus $feHealth 3) {
    "frontend already UP - skip"
} else {
    if (Get-PortOwner 3000) { "port 3000 busy - killing"; Stop-Port 3000 }

    if ($FrontendMode -eq "preview") {
        "building frontend (nuxt build)..."
        Push-Location $frontendDir
        try {
            npm run build 2>&1 | Out-Null
        } finally { Pop-Location }
        if ($LASTEXITCODE -ne 0) { throw "frontend build failed (nuxt build)" }
    }

    $cmd = if ($FrontendMode -eq "preview") { "preview" } else { "dev" }
    $p = Start-Process -FilePath "npm.cmd" -ArgumentList "run", $cmd `
        -WorkingDirectory $frontendDir `
        -RedirectStandardOutput $feOut -RedirectStandardError $feErr `
        -WindowStyle Hidden -PassThru
    "frontend launching ($FrontendMode, PID=$($p.Id))..."
    if (-not (Wait-HttpStatus $feHealth 90)) {
        if ($p.HasExited) { "frontend process EXITED unexpectedly" }
        "--- frontend.err tail ---"; Get-Content $feErr -Tail 25 -ErrorAction SilentlyContinue
        throw "frontend not UP at $feHealth after 90s"
    }
    "frontend UP ($FrontendMode)"
}

# ---------------- registry ----------------
$bePid = Get-PortOwner 8080
$fePid = Get-PortOwner 3000
""
"== service registry =="
"{0,-10} {1,-6} {2,-8} {3}" -f "ROLE", "PORT", "PID", "LOG"
"{0,-10} {1,-6} {2,-8} {3}" -f "backend", "8080", ($(if ($bePid) { $bePid } else { "-" })), $beOut
"{0,-10} {1,-6} {2,-8} {3}" -f "frontend", "3000", ($(if ($fePid) { $fePid } else { "-" })), $feOut
"{0,-10} {1,-6} {2,-8} {3}" -f "postgres", $pgPort, "docker", "(container: postgres)"
