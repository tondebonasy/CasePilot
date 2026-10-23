# API reference

The browser-facing base URL is **http://localhost:8080/api/v1**. Use the gateway for application requests. Protected endpoints require `Authorization: Bearer <accessToken>`.

## Identity and user administration

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/auth/login` | Authenticate an enabled account; returns `accessToken` |
| GET | `/users/me` | Current identity through gateway-injected headers |
| GET / POST | `/admin/users` | List or create staff accounts |
| PUT | `/admin/users/{id}/role` | Change a user's role |
| PUT | `/admin/users/{id}/status` | Enable or disable an account |
| PUT | `/admin/users/{id}/password` | Administrator password reset |

User administration requires an administrator role. Passwords and tokens should not appear in URLs, committed examples, or logs.

## Cases and evidence

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/disputes` | Create a dispute and evaluate eligibility |
| GET | `/disputes?page=0&size=20` | Paginated case summaries permitted for the caller |
| GET | `/disputes/{id}` | Case details, evidence metadata, activity, postings, and flags |
| GET | `/disputes/tasks` | Cases assigned to the caller |
| GET | `/disputes/queue` | Unassigned cases available to the caller's role |
| PUT | `/disputes/{id}/claim` | Claim an available case |
| PUT | `/disputes/{id}/assign` | Assign a case using `assignedToUserId` |
| PUT | `/disputes/{id}/status` | Request an allowed status transition |
| PUT | `/disputes/{id}/reopen` | Reopen where the domain rules permit |
| POST | `/disputes/{id}/evidence` | Multipart upload using the `file` field |
| GET | `/disputes/{id}/evidence/{evidenceId}/download` | Download authorized evidence |
| GET | `/disputes/{id}/evidence/{evidenceId}/view` | View authorized evidence |

The case-list response includes `items`, `page`, `size`, and `totalElements`. Domain services apply role, assignment, ownership, transition, and evidence rules.

### Intake example

The following body illustrates the field names. Replace the transaction timestamp with a recent timestamp when testing eligibility:

```json
{
  "merchantName": "Example Merchant",
  "merchantId": "MERCHANT-001",
  "cardLast4": "1234",
  "amount": 45.50,
  "currency": "USD",
  "txDate": "2026-10-01T10:00:00Z",
  "network": "VISA",
  "transactionRef": "SYNTHETIC-001",
  "rrn": "000000000001",
  "authCode": "TEST01",
  "channel": "E-COMMERCE",
  "terminalId": "WEB-001",
  "terminalLocation": "Example online store",
  "customerIdentifier": "SYNTHETIC-CUSTOMER-001",
  "merchantRefunded": false,
  "disputeType": "NOT_RECEIVED",
  "reason": "The purchased item has not arrived."
}
```

The complete request shape is defined by `DisputeDtos.CreateDisputeRequest`. For a status transition, send a JSON body such as `{"status":"UNDER_REVIEW","detail":"Initial review completed."}`; allowed transitions depend on the current stage and caller. Only administrators can reopen a closed case.

## Operational reports

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/metrics/dashboard` | Portfolio and performance metrics |
| GET | `/metrics/dashboard/export` | Dashboard CSV |
| GET | `/metrics/bank` | Bank reporting summary |
| GET | `/metrics/bank/export` | Bank-report CSV |
| GET | `/notifications` | Notification records for the current user |

Reporting endpoints accept optional `from` and `to` query parameters. See `DateRange` for the accepted date and timestamp parsing rules. Legacy report URLs remain mapped for compatibility; use `/metrics/bank` for new clients.

## Configuration and training

| Method | Path | Purpose |
| --- | --- | --- |
| GET / POST | `/admin/sla` | List or create SLA rules |
| PUT | `/admin/sla/{id}` | Update an SLA rule |
| GET | `/admin/audit` | Administrative audit records |
| GET / POST | `/admin/training/policies` | List or create policies |
| PUT | `/admin/training/policies/{id}` | Update or activate/deactivate a policy |
| GET / POST | `/admin/training/quizzes` | List or create linked quizzes |
| GET | `/training/policies` | Available active policies |
| GET | `/training/quizzes?policyId={id}` | Available quizzes, optionally filtered |
| GET | `/training/quizzes/{id}` | Quiz content |
| POST | `/training/attempts` | Submit quiz answers |
| GET | `/training/attempts/me` | Current user's attempt history |
| GET | `/training/metrics` | Awareness metrics |
| GET | `/training/metrics/export` | Awareness CSV |

A quiz submission contains `quizId` and an `answers` array of `questionId` / `optionId` pairs. Administrative quiz responses include answer correctness for editing; staff-facing quiz DTOs omit the correctness flag.

## Errors

The application distinguishes invalid or expired authentication, forbidden actions, validation failures, and infrastructure failures. Frontend loaders provide retry controls and avoid treating a failed request as a successful empty result. Existing controllers are the authoritative source for request validation and operation-specific behavior; this reference is not an OpenAPI specification.
