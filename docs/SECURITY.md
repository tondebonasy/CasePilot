# Security and deployment

## Implemented controls

- BCrypt password hashing in Auth Service.
- Expiring signed JWTs with issuer verification in the gateway and service filters.
- Role-based administration and service-layer case ownership, assignment, and workflow checks.
- Server-side evidence requirements before submission.
- Upload filename normalization, unique stored names, a configured storage root, and path-boundary checks for downloads.
- A 10 MB multipart limit.
- Activity records for case operations and audit records for selected administrative changes.
- Environment-based signing keys and database credentials; local configuration and runtime files are excluded from Git.
- Formula-prefix protection in the case-table CSV export.

These controls should be assessed together with the deployment environment. They do not establish regulatory certification or independent security assurance.

## Trust boundaries

The gateway validates tokens and forwards identity headers. Backend filters also accept identity headers, so the Auth and Dispute services must remain on a trusted internal network. Direct public access to those services would undermine the intended boundary. Use the gateway as the exposed API entry point and restrict internal service ports.

The browser currently stores the access token and user identity in local storage. The application has no implemented refresh-token or token-revocation flow. Disabling an account does not provide a documented immediate revocation mechanism for tokens already issued. Session storage, expiry, revocation, and XSS defenses need a deployment-specific design.

## Development defaults

The gateway permits wildcard CORS, database URLs use local-development connection options, and Hibernate uses `ddl-auto=update`. The Docker MySQL port is bound to localhost, and new containers require passwords through local configuration. The local administrator seeder is disabled by default.

For deployment, use explicit allowed origins, TLS, managed credentials, least-privilege database accounts, private service networking, and reviewed schema migrations. Keep the seeder disabled and configure the public frontend API address for the deployment environment.

## Evidence and records

Evidence files are stored locally. Encryption at rest, object storage, malware scanning, MIME/content inspection, retention, legal holds, and backup verification are not implemented infrastructure in this repository. Database and filesystem operations do not share an atomic transaction.

Activity and audit records support operational traceability. They are ordinary application database records, not a cryptographically tamper-evident ledger. Administrative audit coverage is not comprehensive across every management action.

## Reporting and operational limits

The client-side case export protects formula-like prefixes; server-side CSV exporters should receive a separate review for spreadsheet injection and sensitive-data handling before external distribution. Reporting currently aggregates records in application memory.

Network submission references and financial postings are internal records. A real deployment would require authenticated network/core-banking adapters, delivery guarantees, reconciliation, idempotency, and failure recovery. Fraud flags are computed from deterministic rules.

## Verification before operational use

Validate the bank's scheme rules and reason mappings, review authorization boundaries, exercise restore procedures, load-test representative data, and establish monitoring and incident handling. Assess dependencies, user privacy requirements, and file handling against the bank's own policies.

The current automated suite covers selected workflow and HTTP behavior against H2. MySQL compatibility, browser automation, penetration testing, and load testing remain separate verification activities.
