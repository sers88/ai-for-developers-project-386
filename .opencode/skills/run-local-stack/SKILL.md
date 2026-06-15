---
name: run-local-stack
description: Brings up / tears down the AiForDevelopers local stack (PostgreSQL via Docker + Spring Boot backend + Nuxt frontend) without dead-air. Use when you need to start, restart, or stop local services, run e2e, or debug service startup. Bundles idempotent PowerShell scripts that poll each service to readiness and print a service registry.
---

# Run Local Stack (no dead-air)

Start / stop / restart the local stack for AiForDevelopers without the user ever
having to say "continue". Also governs any long-running operation (CI waits,
test runs) to prevent the same anti-patterns.

## Why this skill exists

Three anti-patterns previously caused hangs:

- **A. Announce a wait, then stop.** ("waiting for Spring Boot…")
- **B. Launch-and-pray.** (`Start-Process` + fixed `Start-Sleep`, then a bare
  `alive=False` on crash, followed by many slow round-trips.)
- **C. Poll-in-a-loop.** Repeated `Start-Sleep 60; check status` calls, each
  burning a full round-trip, instead of one blocking command that waits to
  completion.

This skill replaces all three with **one self-contained launch block per
service** that polls to readiness (or fails fast with the log tail), and
**blocking calls** for any subsequent wait.

## The 8 rules (operative discipline)

1. **One self-contained launch block.** `Start-Process` → poll the
   ready-endpoint with a timeout → `READY` / `TIMEOUT + log tail`. No separate
   "wait" step.
2. **Never announce a wait — anywhere.** Waiting lives inside the command; never
   in prose. This applies to service startup AND CI polling AND test runs. If
   you catch yourself writing `Start-Sleep` in a loop, stop — use a blocking
   command or a single call with an internal loop instead.
3. **Crash = immediate diagnosis in the same step.** Read stderr, name the
   cause, fix or report — no bare `alive=False`.
4. **Respect resource locks.** Before rebuild / reinstall / delete, stop the
   process holding the file **in the same command**.
5. **Parallelise independent steps.** (e.g. install the Playwright browser while
   servers boot.)
6. **Print a service registry.** role | port | pid | log | status — so context
   is never lost and no "continue" is needed to re-establish it.
7. **After readiness, proceed immediately.** The moment the stack (or CI, or
   tests) reports READY, move to the next step in the SAME response. Do NOT
   output the registry and wait. The registry is context for the user, not a
   stop signal. The only valid stop point is an explicit user gate (e.g. "wait
   for merge command").
8. **Know the tool timeout.** The bash tool has a default timeout of 120000ms
   (2 min). Any command that may run longer MUST set an explicit `timeout`:

   | Command | Timeout |
   |---|---|
   | `gh run watch` (CI wait) | **600000** (10 min) |
   | `npm run test:e2e` | **300000** (5 min) |
   | `npm run build` | **180000** (3 min) |
   | `up.ps1` / `restart.ps1` | **300000** (5 min) |
   | `Start-Sleep -Seconds N` (N > 100) | `N * 1000 + 5000` |

   If a command is killed by timeout, re-run with a larger `timeout` — do NOT
   split it into multiple shorter sleeps.

## Patterns for common waits

### Wait for CI (single blocking call)

```sh
$runId = (gh run list --branch {branch} --limit 1 --json databaseId --jq '.[0].databaseId')
gh run watch $runId --exit-status
```

`gh run watch` blocks until the run finishes. Set `timeout: 600000`. If it
exits non-zero, inspect with `gh run view {id} --log`.

### Wait for local e2e (single blocking call)

```sh
npm run test:e2e
```

Set `timeout: 300000`. If it fails, the error output includes the failing
assertion and screenshot path — fix and re-run in the next step.

### Polling fallback (when no blocking command exists)

```sh
# Single bash call, internal loop, hard cap on iterations:
for ($i = 0; $i -lt 10; $i++) {
  Start-Sleep -Seconds 30
  $r = gh run list --branch {branch} --limit 1 --json status,conclusion --jq '.[0]'
  if ($r -match '"completed"') { $r; break }
}
```

This burns ONE round-trip, not ten.

## Scripts

All scripts resolve the repo root and `.env` themselves — run from anywhere.

| Script | Purpose |
|---|---|
| `scripts/up.ps1 [-FrontendMode dev\|preview]` | Bring up postgres → backend → frontend (default `dev`). Idempotent: skips already-healthy services. |
| `scripts/down.ps1` | Stop frontend (:3000) → backend (:8080) → postgres container. |
| `scripts/restart.ps1 -Target frontend\|backend\|all [-FrontendMode dev\|preview]` | Stop + relaunch one target (or all) with readiness polling. |
| `scripts/status.ps1` | Print what is running (ports, pids, container). |

### Run

```sh
# bring everything up (default: nuxt dev)
pwsh .opencode/skills/run-local-stack/scripts/up.ps1

# preview mode (nuxt build then nuxt preview)
pwsh .opencode/skills/run-local-stack/scripts/up.ps1 -FrontendMode preview

pwsh .opencode/skills/run-local-stack/scripts/restart.ps1 -Target frontend
pwsh .opencode/skills/run-local-stack/scripts/status.ps1
pwsh .opencode/skills/run-local-stack/scripts/down.ps1
```

Logs are written to `%LOCALAPPDATA%\aifordev-run\logs\{backend,frontend}.{out,err}.log`.

## Env mapping (the main gotcha)

The root `.env` sets
`SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/...` for the **Docker
network**. A backend started on the host cannot resolve the `postgres` host, so
`up.ps1` / `restart.ps1` **rebuild** the URL as
`jdbc:postgresql://localhost:<POSTGRES_PORT>/<POSTGRES_DB>` and pass the rest
(user / password / JWT) from `.env`. Every other var (OAuth / SMTP / Calendar)
has a default in `application.properties`, so they do not need to be set.

## Gotchas

- **Frontend dev in SPA mode (`ssr:false`)** requires
  `experimental.viteEnvironmentApi: true` in `nuxt.config.ts` — otherwise
  `nuxt dev` crashes with `No entry found in rollupOptions.input`. Already set.
- **Vite version**: `package.json` has `overrides.vite: ^7.3.5` to avoid a
  dual-vite (8 vs 7) conflict that breaks `nuxt dev`. Do not remove.
- **Google OAuth / SMTP** are not configured locally → use email/password auth;
  emails are not sent.
- **Playwright e2e** needs the full stack up plus the chromium browser
  (`npx playwright install chromium`). `up.ps1` does NOT run e2e — after the
  stack is up, run `npm run test:e2e` in `frontend/` (see "After the stack is
  up" below).
- Backend `bootRun` runs the app inside the Gradle daemon JVM, so the process
  owning port 8080 is the daemon itself — stopping that PID cleanly frees the
  port (no orphaned wrapper).

## After the stack is up

`up.ps1` prints a service registry and exits. **Do NOT stop here.** Proceed
immediately to whatever task needed the stack:

| Task | Next command | Workdir | Timeout |
|---|---|---|---|
| Run e2e | `npm run test:e2e` | `frontend/` | 300000 |
| Run backend tests | `./gradlew test` | `backend/` | 300000 |
| Run frontend unit tests | `npm run test` | `frontend/` | 120000 |
| Manual debugging | `npm run dev` (if not already in preview mode) | `frontend/` | — |

**The #1 cause of dead-air is stopping after `up.ps1` output.** The service
registry is for the user's context, not a signal to pause. The very next tool
call after `up.ps1` should be the task that required the stack (install
Playwright, run e2e, etc.), not a `Start-Sleep` or a status check.
