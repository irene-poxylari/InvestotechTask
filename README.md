- # AccountTransferTask

A REST API for authenticated transfers between accounts using Java 21, Spring Boot, and MySQL.

The application supports API-key login, JWT authentication, account listing, same-currency transfers, and paginated transfer history. Monetary values are stored and exchanged as integer minor units: for example, `1000` represents EUR 10.00.

## Technology

- Java 21 and Spring Boot 4.1.1
- Spring Web, Spring Security, and Bean Validation
- Spring Data JPA / Hibernate
- MySQL 8.4
- Flyway database migrations
- JJWT for token creation and verification
- JUnit Jupiter and Mockito
- Maven Wrapper and Docker Compose

## Run locally

Requirements: JDK 21, Docker with Compose, and ports 8080 and 8085 available. Maven is supplied through the wrapper; the first build needs access to download dependencies.

Run all commands from the project root. The examples below use Windows PowerShell.

### 1. Start MySQL

```powershell
docker compose up -d mysql
docker compose logs mysql
```

Wait until MySQL reports that it is ready for connections. The supplied Compose configuration exposes MySQL on port **8085** and creates the **testdb** database.

Use a fresh evaluation database. An existing database previously managed by Hibernate may need an explicit migration plan before Flyway can manage it.

### 2. Configure the application

```powershell
$env:DB_URL = "jdbc:mysql://localhost:8085/testdb"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "mypassword"

# Generate a random signing secret for this local session.
$keyBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($keyBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($keyBytes)

# Enable the evaluation users and accounts.
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:APP_SEED = "true"
```

The database credentials above match the supplied local Docker configuration. They are demonstration credentials, not production settings.

Keep the JWT secret stable during an evaluation session. Changing it invalidates previously issued tokens.

### 3. Start the API

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--app.seed=true --spring.main.web-application-type=none"
```

The API listens at `http://localhost:8080`.

Flyway loads migrations from `src/main/resources/db/migration`. Hibernate uses `ddl-auto=validate` to check the resulting schema.

On macOS or Linux, use equivalent `export` commands and `./mvnw`.

### Running from IntelliJ IDEA

1. Open the project as a Maven project and select JDK 21.
2. Reload Maven dependencies.
3. Open **Run → Edit Configurations** and select `AccountTransferTaskApplication`.
4. Add `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` under **Environment variables**.
5. For demonstration data, also set `SPRING_PROFILES_ACTIVE=dev` and `APP_SEED=true`.
6. Run the application.

Variables set in a separate terminal do not automatically update an already-running IntelliJ run configuration.

## Demonstration data

Seeding requires both the `dev` profile and `app.seed=true`. It creates missing users and accounts; it does not reset existing balances or replace existing users' API-key hashes.

| User | API key | Account | Currency | Initial balance in minor units |
|---|---|---|---|---:|
| Alice | `alice-key` | `alice-eur` | EUR | 100000 |
| Alice | `alice-key` | `alice-usd` | USD | 50000 |
| Bob | `bob-key` | `bob-eur` | EUR | 25000 |

Default configuration disables seeding.

## API walkthrough

All endpoints except login require:

- Amount must be positive and fit the request's Java `int` range.
- The source must have sufficient funds.
- The destination balance must not overflow.
- No currency conversion is performed.

The idempotency key must be nonblank and at most 200 characters. Keys are scoped to the authenticated user. Repeating the same key and request returns the original transfer without another debit. Reusing the key with a changed request returns **409 Conflict**. The current controller returns 201 for successful replays as well.

### Transfer history

```http
GET /accounts/alice-eur/transfers?limit=20
```

The caller must own the requested account. The default limit is 20; its declared valid range is 1–100.

Responses contain `items` and `next_cursor`. Items are ordered by creation time descending, then transfer ID descending. Each item has a `direction` of `DEBIT` or `CREDIT` relative to the requested account.

```powershell
$page = Invoke-RestMethod -Uri "$baseUrl/accounts/alice-eur/transfers?limit=20" -Headers $authHeaders
$page.items

if ($page.next_cursor) {
    $cursor = [Uri]::EscapeDataString($page.next_cursor)
    Invoke-RestMethod -Uri "$baseUrl/accounts/alice-eur/transfers?limit=20&cursor=$cursor" -Headers $authHeaders
}
```

Treat cursors as opaque. A null `next_cursor` indicates the last page.

## Errors

Handled application errors use this structure:

```json
{
  "code": "insufficient_funds",
  "message": "Source account has insufficient funds",
  "fields": {},
  "timestamp": "2026-01-01T12:00:00Z"
}
```

| Status | Examples |
|---|---|
| 400 | Invalid request body, missing idempotency key, invalid cursor |
| 401 | Missing or invalid API key; missing, invalid, or expired bearer token |
| 403 | Source or history account belongs to another user |
| 404 | Account not found |
| 409 | Idempotency key reused with a different request |
| 422 | Insufficient funds, currency mismatch, same-account transfer, balance overflow |

## Design

Controllers handle HTTP input and delegate to services. Repositories handle persistence and database-specific locking. Account entities enforce positive debit/credit amounts and sufficient balance.

Transfer creation uses a single transaction covering account updates, the transfer record, and its idempotency record. Accounts are retrieved with pessimistic write locks and ordered by ID. A separate idempotency lock serializes requests sharing a user/key pair. The lock repository requires an existing transaction.

History uses cursor pagination rather than offset pagination. Constructor injection makes service dependencies explicit and permits isolated unit tests.

These mechanisms are intended to protect transfer consistency. Their behavior under real database concurrency must be verified through integration tests.

## Tests

Run the service unit tests without starting MySQL or configuring credentials:

```powershell
.\mvnw.cmd "-Dtest=TransferServiceTest" test
```

The supplied suite contains 27 cases covering balances, idempotency, authorization, currency rules, insufficient funds, overflow reporting, persistence failures, and history pagination. Repositories are mocked; the hash function, cursor codec, and entities are real objects.

Run all tests only after configuring an isolated test database and the required environment variables:

The existing `@SpringBootTest` context test starts the application context and uses the configured database. It does not provision its own isolated database.

Unit tests do not prove transaction rollback, SQL compatibility, or concurrent locking. MySQL integration tests are required for those guarantees.

## Submission status and limitations

The reviewed archive has two outstanding issues that should be resolved before evaluation:

- `AuthService.login()` passes the raw API key to `findByApiKeyHash()`. Hash it with `apiKeyHasher.sha256(apiKey)` before the lookup so the login walkthrough works.
- Idempotency-key validation was moved into the mocked lock repository. Restore validation in the service, or introduce a separately tested validator and adapt the tests. The supplied invalid-key and no-interaction tests do not match the current refactoring.

Additional limitations:

- Constructor validation for negative initial account balances is incomplete.
- Query-parameter validation errors still need consistent HTTP error mapping.
- No passing test run is claimed here: verification encountered local dependency-file access errors.
- No automated MySQL concurrency/rollback suite is included.
- Idempotency records have no defined expiration or cleanup policy.
- JWT logout/revocation, login rate limiting, and production audit/reconciliation facilities are outside the current implementation.
- The Compose database uses a demonstration root credential. Production deployment requires restricted database credentials, managed secrets, and HTTPS.

## Troubleshooting

| Symptom | Check |
|---|---|
| Cannot resolve `JWT_SECRET` | Set it in the environment of the process actually launching the application. |
| Schema validation reports missing tables | Confirm Maven dependencies are loaded, Flyway startup logs show the migration running, and `DB_URL` identifies the intended database. |
| Login returns “Invalid API key” | Confirm login hashes the key, demo data exists, and the API and SQL client use the same database instance and port. |
| Demo accounts are missing | Enable both the `dev` profile and `APP_SEED=true`, then restart. |
| A previously valid JWT returns 401 | Log in again; tokens expire and secret changes invalidate old tokens. |

Before submitting, resolve the two listed blockers, run the tests, and update this status section with the actual results. Include the source, Maven wrapper, Compose file, and migrations. Exclude build outputs, IDE-specific files, and private credentials.

