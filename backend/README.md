# Backend services

CasePilot runs three independent Java 17 / Spring Boot applications:

| Service | Port | Responsibility |
| --- | --- | --- |
| API Gateway | 8080 | Browser API routing and token verification |
| Auth Service | 8081 | Accounts, credentials, roles, and JWT issuance |
| Dispute Service | 8084 | Cases, evidence, deadlines, reporting, and training |

Use the repository-level [setup guide](../docs/SETUP.md) for configuration, MySQL, the first administrator, and run commands. Each service has its own `pom.xml`. Run `mvn test` from a service directory, or `mvn -f backend/<service>/pom.xml test` from the repository root.

Dispute-service integration tests use H2 and test-only configuration. The local application uses separate `auth_db` and `dispute_db` MySQL databases. Uploaded evidence is runtime data and is excluded from Git.

See [architecture](../docs/ARCHITECTURE.md), [API reference](../docs/API.md), and [security considerations](../docs/SECURITY.md).
