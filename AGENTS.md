# AGENTS.md

## Architecture

Monorepo: **backend** (Spring Boot 3.4, Kotlin 2.1, Gradle 8.13, Java 21) + **frontend** (Nuxt 3.15, Vue 3.5, Node 22, SPA mode `ssr: false`). PostgreSQL 16 via Docker Compose.

## Commands

### Backend (`backend/`)
```sh
./gradlew build                  # ktlint + compile + test
./gradlew test                   # tests only
./gradlew ktlintCheck            # lint only
./gradlew ktlintFormat           # auto-fix lint (run after writing Kotlin)
./gradlew build -x test          # compile only (CI uses this)
```

### Frontend (`frontend/`)
```sh
npm run dev                      # dev server
npm run lint                     # ESLint
npx nuxt prepare                 # generate .nuxt types → needed before typecheck
npm run typecheck                # vue-tsc
npm run test                     # vitest
npm run build                    # production build
```

## Critical gotchas

### Backend
- **ktlint is strict**: trailing commas required, multiline lambdas need `{` on new line. Always run `ktlintFormat` after writing Kotlin code, then `build`.
- **Tests use `@TestPropertySource`**, not `application-test.yml`. The profile-based config was unreliable — inline properties are used instead. H2 in PostgreSQL mode, Flyway disabled.
- **`kotlin.test.*` needs `testImplementation("org.jetbrains.kotlin:kotlin-test")`** in `build.gradle.kts` — it's not included by default.
- **`allOpen` plugin** is required for JPA entities (`kotlin("plugin.jpa")` + kotlin("plugin.spring")).
- **JWT library**: `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6` — api is `implementation`, impl+jackson are `runtimeOnly` to avoid leaking.

### Frontend
- **`.vue` SFCs must use `<script setup lang="ts">`** — bare `<script setup>` causes vue-tsc errors.
- **`npx nuxt prepare` before `npm run typecheck`** — generates `.nuxt/tsconfig.json` with auto-import paths.
- **ESLint is strict**: no unused imports, no `any` type, no self-closing on void HTML elements (`<input />` → `<input >`).
- **VeeValidate `<Form>`/`<Field>` components don't render in vitest** → test validation logic directly with `useForm()` composable instead.
- **Vitest config** needs `@vitejs/plugin-vue` and `environment: "happy-dom"`.
- **`@vee-validate/zod`** must be installed alongside `zod@3` (zod@4 is incompatible).

### CI
- **Backend CI runs lint + compile only** — tests are skipped with `-x test`.
- **Frontend CI** runs lint → `nuxt prepare` → typecheck → build. No vitest step.
- **Order matters**: lint → prepare → typecheck → build.

## Environment
- `.env` (gitignored) holds Docker Compose variables.
- `.env.example` is the template — update both when adding vars.
- Backend env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_JPA_HIBERNATE_DDL_AUTO`, `JWT_SECRET`.
- Google OAuth vars: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, `FRONTEND_URL`.
- Frontend env var: `NUXT_PUBLIC_API_BASE`.

## Google OAuth
- **Flow**: user clicks "Sign in with Google" → frontend redirects to `GET /api/auth/oauth2/google` → backend redirects to Google → Google redirects to `GET /api/auth/oauth2/google/callback?code=...` → backend exchanges code, creates/finds user, issues JWT → redirects to `{FRONTEND_URL}/auth/callback?accessToken=...&refreshToken=...&email=...`
- **User entity**: `password_hash` is nullable (OAuth users have no password). `google_id` (unique), `name`, `avatar_url` are optional fields.
- **Linking**: if a user exists by email (from password registration), Google OAuth links the `google_id` and updates `name`/`avatar_url`. If user exists by `google_id`, logs in directly.
- **`@EnableConfigurationProperties(GoogleOAuth2Properties::class)`** is on `AiForDevApplication` — properties prefix: `app.oauth2.google`.
- **Tests**: `OAuthIntegrationTest` mocks `OAuth2Service` via `@MockitoBean`. Integration tests require `app.oauth2.google.*` properties in `@TestPropertySource`.

## Auto-imports (Nuxt)
- `composables/use*.ts` → available without import in `.vue` files
- `stores/*.ts` → Pinia stores auto-imported via `@pinia/nuxt`
- `middleware/*.ts` → auto-discovered by Nuxt router
