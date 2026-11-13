# DigitalTimeline

AI-powered unified academic and digital productivity timeline. It pulls activity from GitHub, Gmail, Google Calendar and (account permitting) Google Classroom into one chronological timeline, keeps confirmed facts separate from suggestions, and answers questions only from your own records.

## IMPORTANT: current status (please read)
All code was written in an environment with no network, no Java, no Maven and no Node. So **nothing has been compiled, started or tested**, and **no provider has been tested against its real API**. Expect to fix some compile or runtime errors on first run. Every test file is written but **none has been executed**. See "Testing" and "Known limitations".

## Tech stack
Backend: Java 21, Spring Boot 3.3, Spring Security, JWT, JdbcTemplate (no JPA/Hibernate), PostgreSQL, Flyway, Jackson, Spring Scheduler/async, SSE, Maven.
Frontend: React 18, Vite, React Router, Framer Motion, Lucide, Recharts, custom CSS (no component library).

## Architecture
Browser (React) -> `/api` (Vite dev proxy) -> Spring Boot: Controller -> Service/Repository (JdbcTemplate) -> PostgreSQL.
Background: scheduler queues syncs -> thread pool runs one `ActivityConnector` per provider -> activities saved (duplicates ignored) -> `SyncFinishedEvent` -> notification saved + live update pushed over SSE.
Two separate kinds of auth: (1) your login to this app (JWT access token + rotating refresh token), (2) OAuth 2.0 authorization to GitHub/Google, done on the server; secrets and provider refresh tokens never reach React.

## Java concepts and where they are used
- Interfaces / Strategy: `ActivityConnector`, `AiProvider`. Polymorphism: four connectors, two AI providers.
- Encapsulation and records: DTOs, `ImportedActivity`.
- Collections: `Map<Provider,ActivityConnector>`, `ConcurrentHashMap`, `HashSet`, `LinkedHashMap`, `PriorityQueue` (priority engine).
- Custom exceptions: `OAuthException`, `SyncException`, `ResourceNotFoundException`, `UnauthorizedIntegrationException`.
- Lambdas and Streams: filtering, grouping, ranking.
- Concurrency: thread pool plus a concurrent set so only one sync runs per user and provider.
- File handling: CSV and JSON export. JDBC: JdbcTemplate insert/update/delete/select, batch, transactions. Jackson for JSON.
- Patterns: Observer (Spring events), Adapter (`GithubApi`, `GoogleApi`), Factory-style lookup (connector map in `SyncService`).

## Database (Flyway runs automatically at startup)
`users`, `refresh_tokens` (hashed), `integrations` (one row per user and provider), `oauth_tokens` (encrypted), `activities` (unique on user + source + external_id, full-text index), `tasks` (`is_suggestion` flag), `task_sources`, `projects`, `project_activities`, `activity_relations`, `sync_state` (cursor), `sync_history`, `notifications`, `ai_conversations`, `ai_messages`. All user data cascades on user deletion. Migrations: `backend/src/main/resources/db/migration` (V1, V2).

## Folder structure
```
digitaltimeline/
  .env.example  .gitignore  README.md
  backend/  pom.xml
    src/main/java/com/digitaltimeline/
      config security auth user integration activity sync task project timeline
      analytics ai notification export exception util dashboard
    src/main/resources/  application.yml, db/migration/
    src/test/java/...
  frontend/  package.json vite.config.js index.html
    src/  api.js styles.css App.jsx auth/ landing/ pages/ app/ test/
```

## Prerequisites
Java 21, Maven 3.9+, Node 20+, PostgreSQL 14+. No Docker.

## Setup
1. Create the database: `createdb digitaltimeline` (or create a database with that name in pgAdmin).
2. Copy `.env.example` to `.env` and fill it in. Generate secrets with `openssl rand -base64 48` (JWT_SECRET, 32+ characters) and `openssl rand -base64 32` (TOKEN_ENC_KEY, must decode to exactly 32 bytes).
3. Spring does not read `.env` by itself, so load it into the terminal first.
   - Mac/Linux: `set -a; source .env; set +a`
   - Windows PowerShell: `Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object { $k,$v = $_ -split '=',2; [Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim()) }`
4. Backend: `cd backend` then `mvn spring-boot:run` (http://localhost:8080). Flyway creates the tables on first start.
5. Frontend, in a second terminal: `cd frontend`, `npm install`, `npm run dev` (http://localhost:5173).

## Environment variables
Required: `DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, TOKEN_ENC_KEY`.
Optional: `CORS_ORIGIN, FRONTEND_URL, GITHUB_REDIRECT_URI, GOOGLE_REDIRECT_URI_TEMPLATE, GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, GITHUB_SCOPES, GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GEMINI_API_KEY, GEMINI_MODEL`.
The app runs without any provider or AI key; the Connect buttons then explain what is missing.

## GitHub Codespaces
The URL settings are environment-driven so the same code works locally and in Codespaces. In your Codespace, set these values in the root .env file (do not commit .env):

- CORS_ORIGIN and FRONTEND_URL: the forwarded frontend URL (the port your Vite app uses, commonly 5173 or 5176).
- GITHUB_REDIRECT_URI: the forwarded backend URL (port 8080) plus /api/integrations/github/callback.
- GOOGLE_REDIRECT_URI_TEMPLATE: the same forwarded backend URL plus /api/integrations/{provider}/callback.

In GitHub Developer Settings, set the OAuth App callback URL to the exact GITHUB_REDIRECT_URI. In Google Cloud, add three authorized redirect URIs by replacing {provider} with gmail, calendar and classroom. Make the backend port reachable by the browser's OAuth redirect. If the Codespace is recreated or its forwarded hostname changes, update these environment values and the provider console URLs together. Never paste OAuth secrets into chat or commit them.

## GitHub OAuth setup
GitHub > Settings > Developer settings > OAuth Apps > New. Homepage `http://localhost:5173`, callback `http://localhost:8080/api/integrations/github/callback`. Put the Client ID and a new secret in `.env`. Default scope is `read:user public_repo` (public repositories). For private repositories set `GITHUB_SCOPES="read:user repo"`; GitHub OAuth Apps have no read-only private-repository scope, so `repo` also grants write access. Disconnect deletes our stored token; revoke the app in GitHub settings to fully remove access.

## Google OAuth setup (Gmail, Calendar, Classroom)
1. console.cloud.google.com: create a project and enable Gmail API, Google Calendar API and optionally Google Classroom API.
2. OAuth consent screen: External, publishing status Testing, and add your own account as a Test user.
3. Credentials > OAuth client ID > Web application. Add redirect URIs matching `GOOGLE_REDIRECT_URI_TEMPLATE` for `gmail`, `calendar` and `classroom`.
4. Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, restart, then Connect each service separately.
5. In Testing mode Google expires refresh tokens after 7 days. The Integrations page then shows an error and a Reconnect button. Google also shows an "unverified app" warning; that is normal here.
6. Scopes: `gmail.readonly`, `calendar.events.readonly`, `classroom.courses.readonly` and `classroom.coursework.me.readonly`. Classroom only works for accounts enrolled in courses; otherwise the UI says no courses are visible.

## AI assistant
Works with no key: it classifies the question, retrieves real records and formats them, listing every source. Optional Gemini (set `GEMINI_API_KEY`, then tick the box in the Assistant) receives only titles, kinds, dates and short notes, never email content. The model name comes from `GEMINI_MODEL`; check Google's current free-tier models and data terms.

## Live updates, polling, settings
Polling defaults: GitHub 2 min, Gmail 5, Calendar 10, Classroom 10 (`app.sync-intervals-ms.*`). This is polling, not push. After each sync an SSE event refreshes open pages. Settings cover profile, auto-sync switch, password change (signs out everywhere), JSON/CSV export and account deletion.

## Troubleshooting
- `Could not resolve placeholder 'DB_PASSWORD'` or `'JWT_SECRET'`: variables are not loaded in this terminal (Setup step 3).
- `TOKEN_ENC_KEY must be base64 of exactly 32 bytes`: regenerate with `openssl rand -base64 32`.
- Flyway or connection errors: check the database exists and `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- `redirect_uri_mismatch`: the URI in the provider console must match the corresponding `GITHUB_REDIRECT_URI` or Google callback URL exactly, including scheme, hostname and port.
- Blank page or errors on `/api`: the backend is not running on port 8080.
- Live updates silent: check the backend log for authentication errors on `/api/events`.

## Testing
Backend (written, not run): `cd backend` then `mvn test`. Covers JWT, token encryption, OAuth state, Google URL building, CSV, priority engine, AI grounding, sync locking and errors, account actions, login rate limit.
Frontend (written, not run): `cd frontend` then `npm test`. Covers form validation, login flow, server error display, token refresh.
Not written: database repository tests, controller (MockMvc) tests, an end-to-end duplicate-import test against PostgreSQL, SSE tests. Provider integrations need manual testing with your own accounts.

## Security notes
BCrypt passwords; 15-minute JWTs; hashed rotating refresh tokens; AES-256-GCM provider tokens; one-time provider-bound OAuth `state`; every query filtered by user id; input validation; generic 500 errors; login limited to 5 failures per email per 15 minutes (in memory, per instance); CORS limited to the frontend origin. Tradeoffs: the refresh token lives in `localStorage` (XSS would expose it; an HttpOnly cookie is the production fix) and registration reveals whether an email exists.

## Known limitations
- Missing: activity detail page, task edit/detail view, project timeline, activity-to-project linking UI, related-activity indicators, PDF export.
- Gmail: metadata only, 100 messages per sync; History API and thread IDs not implemented.
- Calendar: primary calendar only; edits to already-imported events are not re-imported.
- Tasks have no completion date, so productivity trends are activity-based only.
- Task suggestions are not yet generated automatically from emails or events (the suggestion flag and confirm flow exist).
- No per-provider sync preferences. Classroom depends on your account.
- Not deployed or load-tested; no HTTPS handling.
