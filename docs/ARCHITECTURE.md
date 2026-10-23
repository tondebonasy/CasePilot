# Architecture

## Components

| Component | Owns | Dependencies |
| --- | --- | --- |
| Angular frontend | Sign-in, dashboard, case views, evidence interactions, training, administration | Material, reactive forms, RxJS |
| API Gateway | Routing, token verification, identity headers, browser CORS | Spring Cloud Gateway, JJWT |
| Auth Service | Accounts, password hashes, roles, enabled status, token creation | Spring Security, JPA, `auth_db` |
| Dispute Service | Case lifecycle, assignment, evidence metadata, postings, alerts, training, reports | JPA, `dispute_db`, filesystem |

The backend modules are independent Maven applications that communicate synchronously over HTTP.

## Request lifecycle

1. The frontend posts credentials to `/api/v1/auth/login` through the gateway.
2. Auth Service checks the enabled account and BCrypt password hash, then issues an expiring signed JWT.
3. The frontend requests `/api/v1/users/me` and stores the identity with the access token.
4. An interceptor adds the bearer token to protected requests.
5. The gateway verifies the token and forwards identity headers to the selected service.
6. Service filters establish authentication; controllers and domain services enforce role and case rules.
7. The frontend distinguishes expired sessions, permission failures, and temporary loading failures.

The token issuer is shared across services. Its legacy identifier is an internal compatibility value; changing it requires coordinated configuration and new sign-ins.

## Data ownership

`auth_db.users` stores email, full name, password hash, role, enabled status, and creation time. Case records reference users by ID rather than a cross-database JPA relationship.

| Operations data | Relationships and purpose |
| --- | --- |
| Transactions | Reference, merchant, amount, currency, network, date, and status |
| Disputes | Transaction, eligibility, status, opener, assignee, deadlines, submission, closure |
| Evidence | Case, original filename, content type, stored path, uploader, timestamp |
| Activity | Case, actor, action, details, timestamp, IP information |
| SLA rules | Internal and network windows by network and dispute type |
| Financial postings | Temporary credit, final credit, and reversal records |
| Exceptions | Fraud flags and notifications associated with cases and recipients |
| Training | Policies → quizzes → questions → options; attempts and submitted answers |
| Administration | Audit records for selected administrative changes |

Some tables and Java packages retain legacy names to preserve mappings and compatibility. Product labels and report filenames use CasePilot and bank-neutral wording.

## Evidence storage

`StorageService` normalizes a configured root and creates a directory per dispute. It replaces unsafe filename characters and adds a UUID prefix. Relational metadata retains the original filename, content type, uploader, timestamp, and stored path.

Downloads reject paths outside the storage root and require case access. Multipart request and file limits default to 10 MB. Filesystem writes are separate from database transactions; orphan-file cleanup is a deployment concern.

## Deadlines

Intake selects the SLA rule matching network and dispute type. When no rule exists, the implemented fallback is 10 internal days and 16 network days. A past transaction date becomes the deadline base; otherwise the service uses the current time.

The monitor checks open cases for internal and network deadlines, creates activity and in-app notification records, and marks breaches. Alert timestamps prevent repeated alerts at the same threshold. The default configured scan interval is 10 seconds; the approaching-deadline window is 72 hours.

Eligibility time limits and reason mappings are defined in application code and require bank review before operational use.

## Reporting and scale

Dashboard and bank reports cover disputes opened in a requested date range. Training reports aggregate scores and completions by policy and role. Current operational reporting loads matching records and aggregates them in Java. Larger datasets would benefit from database aggregation, measured index design, and explicit performance targets.

The web case list loads a page of records. Search, status filtering, and its CSV export apply to those loaded records, not the entire database.

## Source layout

```text
backend/api-gateway/       Routing and identity propagation
backend/auth-service/      Accounts and token issuance
backend/dispute-service/   Domain services and integration tests
backend/mysql-init/        Database creation and application grants
frontend/casepilot/        Angular workspace
scripts/                   Local configuration helper
docs/                      Product and technical documentation
.github/workflows/         Build and test automation
```

## Test coverage

Service tests exercise deadlines, status rules, evidence gates, and training scoring. MockMvc tests exercise authenticated read endpoints, policy and quiz creation, report responses, and denied admin access for a customer role. Test properties isolate persistence in H2 and use a test-only signing key. They do not validate MySQL query plans, external payment networks, malware scanning, or browser interactions.
