---
name: run-local-stack
description: Brings up / tears down the AiForDevelopers local stack (PostgreSQL via Docker + Spring Boot backend + Nuxt frontend) without dead-air. Use when you need to start, restart, or stop local services, run e2e, or debug service startup. Bundles idempotent PowerShell scripts that poll each service to readiness and print a service registry.
---

# Run Local Stack (no dead-air)

Start / stop / restart the local stack for AiForDevelopers without the user ever
having to say "continue".

## Why this skill exists

Two anti-patterns previously caused hangs when starting things:

- **A. Announce a wait, then stop.** ("waiting for Spring Boot…")
- **B. Launch-and-pray.** (`Start-Process` + fixed `Start-Sleep`, then a bare
  `alive=False` on crash, followed by many slow round-trips.)

This skill replaces both with **one self-contained launch block per service**
that polls to readiness (or fails fast with the log tail).

## The 6 rules (operative discipline)

1. **One self-contained launch block.** `Start-Process` → poll the
   ready-endpoint with a timeout → `READY` / `TIMEOUT + log tail`. No separate
   "wait" step.
2. **Never announce a wait.** Waiting lives inside the command; never in prose.
3. **Crash = immediate diagnosis in the same step.** Read stderr, name the
   cause, fix or report — no bare `alive=False`.
4. **Respect resource locks.** Before rebuild / reinstall / delete, stop the
   process holding the file **in the same command**.
5. **Parallelise independent steps.** (e.g. install the Playwright browser while
   servers boot.)
6. **Print a service registry.** role | port | pid | log | status — so context
   is never lost and no "continue" is needed to re-establish it.

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
  stack is up, run `npm run test:e2e` in `frontend/`.
- Backend `bootRun` runs the app inside the Gradle daemon JVM, so the process
  owning port 8080 is the daemon itself — stopping that PID cleanly frees the
  port (no orphaned wrapper).
