# Setup and troubleshooting

## Requirements

| Tool | Version / purpose |
| --- | --- |
| JDK | 17; used for all backend compilation and run configurations |
| Maven | 3.9+; each service is a separate Maven project |
| Node.js | 20 |
| npm | Supplied with Node.js; use the committed lockfile with `npm ci` |
| MySQL | 8; installed locally or provided by Docker Compose |
| Docker Compose | v2, only when using the supplied database container |

Verify `java -version`, `mvn -version`, `node --version`, and `npm --version`. Maven should report the same Java 17 installation used by the IDE.

## Local configuration

From the repository root:

```powershell
Copy-Item .env.example .env
```

Generate random configuration values separately. This command prints one random value; run it again for each independent password or secret you need:

```powershell
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

Edit `.env` locally. Keep it out of source control.

| Variable | Required by | Meaning |
| --- | --- | --- |
| `JWT_SECRET` | All backend services | Same random signing key across services; at least 32 UTF-8 bytes |
| `DB_USERNAME` | Auth and Dispute services | Database account; `app` for the supplied Docker setup |
| `DB_PASSWORD` | Auth and Dispute services; Docker | Password for the application database account |
| `MYSQL_ROOT_PASSWORD` | Docker only | Separate root password for a new MySQL container |
| `SEED_ADMIN_ENABLED` | Auth Service | `true` only when creating the first local administrator |
| `SEED_ADMIN_PASSWORD` | Auth Service, when seeding | Password chosen for `admin@casepilot.local` |

Import the file into each PowerShell terminal that will start a backend service:

```powershell
. .\scripts\Import-LocalEnvironment.ps1
```

The helper accepts `NAME=value` lines, blank lines, and full-line comments. Optional matching outer quotes are removed; values are never evaluated as shell code. Imported variables apply to the current terminal and its child processes. The helper validates required application settings and does not print secret values.

Spring Boot does not automatically read the root `.env`. Importing it in one terminal does not configure unrelated terminals or IntelliJ run configurations.

On a POSIX shell, use a shell-compatible local environment file and export its entries before starting services. The same variables and service commands apply.

## Database option A: Docker Compose

Set `DB_USERNAME=app`, `DB_PASSWORD`, and `MYSQL_ROOT_PASSWORD` before creating the container:

```powershell
docker compose --env-file .env -f backend/docker-compose.yml up -d --wait mysql
```

The database initialization creates `auth_db` and `dispute_db` and grants the `app` account access to both. The MySQL port is bound to localhost. The named volume preserves data between container restarts.

The Compose volume retains its existing internal name for compatibility. Do not delete it to resolve a configuration problem. MySQL initialization scripts and account creation run only when the data directory is first initialized; changing `.env` does not reset passwords in an existing database.

If another MySQL instance already owns port 3306, use it or stop that instance intentionally before starting the container. Do not run two servers on the same port.

## Database option B: existing MySQL

Create `auth_db` and `dispute_db` in your installed MySQL instance. Configure an application account with privileges on those databases and use its credentials in `DB_USERNAME` and `DB_PASSWORD`. The supplied initialization SQL assumes the Docker-created `app` account; adapt grants deliberately when using a different account.

Development uses Hibernate `ddl-auto=update` to create or update tables. Production schema changes should use reviewed migrations.

## Start the services

Open separate terminals in the repository root, import `.env` in each backend terminal, and run:

```powershell
mvn -f backend/auth-service/pom.xml spring-boot:run
```

```powershell
mvn -f backend/dispute-service/pom.xml spring-boot:run
```

```powershell
mvn -f backend/api-gateway/pom.xml spring-boot:run
```

For the frontend:

```powershell
npm --prefix frontend/casepilot ci
npm --prefix frontend/casepilot start
```

Open **http://localhost:4200**. Browser requests go to the gateway at port **8080**. Auth Service listens on **8081**; Dispute Service listens on **8084**.

The optional Windows launcher inherits variables from its terminal. Import `.env` first, then run `.\run-app.bat`. It opens a console for each component and skips ports already in use; it does not reload an already-running service. Prefer the separate-terminal commands when diagnosing startup problems.

## Create the administrator

1. Set `SEED_ADMIN_ENABLED=true` and a strong `SEED_ADMIN_PASSWORD` locally.
2. Import the updated file and start or restart Auth Service.
3. Sign in as `admin@casepilot.local` with that password.
4. Set `SEED_ADMIN_ENABLED=false` and restart Auth Service after importing the change.

The seeder skips an account if its email already exists. It does not reset an existing password. Use the authenticated administrator's user-management screen to reset another account's password.

## IntelliJ IDEA

Import the three backend `pom.xml` files as Maven projects. Set Project SDK, module SDK, Maven importer JDK, Maven runner JRE, and each application run configuration to Java 17.

Configure environment variables on each run configuration, not just the gateway:

- **Gateway:** `JWT_SECRET`.
- **Auth Service:** `JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`; seed variables when required.
- **Dispute Service:** `JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`.

Run `ApiGatewayApplication`, `AuthServiceApplication`, and `DisputeServiceApplication`. Use the same issuer and secret in all services.

## Build and verification

```powershell
npm --prefix frontend/casepilot run build -- --configuration production
mvn -f backend/api-gateway/pom.xml test
mvn -f backend/auth-service/pom.xml test
mvn -f backend/dispute-service/pom.xml test
```

Production frontend files are written to `frontend/casepilot/dist/casepilot/browser`. The test suite runs against H2; no local MySQL password is required for those tests. Auth Service and API Gateway currently have compilation checks but no dedicated test classes. Frontend validation currently consists of production compilation and manual browser checks.

## Troubleshooting

| Symptom | Check and resolution |
| --- | --- |
| `Could not resolve placeholder 'JWT_SECRET'` | Import `.env` in that terminal or set the variable on that exact IDE run configuration. |
| `TypeTag :: UNKNOWN` during compilation | Use Java 17 consistently; a newer compiler may be incompatible with the project's annotation processor version. |
| Port already in use | Identify the owning process and stop the duplicate intentionally. Do not change one service's port without updating its gateway route. |
| Policies, quizzes, or dashboard return 500 through the gateway | Confirm Dispute Service runs on 8084 and restart the gateway after changing routes. A running service retains its startup configuration. |
| Report route missing after an update | Rebuild and restart Dispute Service as well as the gateway. |
| Database access denied | Verify credentials against the actual MySQL instance; changing Docker environment values does not change existing account passwords. |
| JWT rejected after configuration changes | Ensure all services use the same secret and issuer, restart affected services, and sign in again. |
| Empty policy or quiz list | Expected on a fresh database; create an active policy, then a linked quiz. |
| Old branding or broken asset still appears | Start Angular from `frontend/casepilot`, stop any server from the old folder, then hard-refresh the browser. |
| Google Fonts unavailable | Compilation does not inline remote fonts. Runtime font loading still needs access to Google Fonts; text uses the configured fallback font when unavailable. |
