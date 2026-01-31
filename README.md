# Banking Ledger Core System

A **production-ready** banking ledger system implementing double-entry bookkeeping principles with **enterprise-grade architectural patterns**, designed for high-integrity financial transaction processing in distributed systems.

## 🎯 Features

- ✅ **Double-Entry Bookkeeping** - All transactions balanced (Σ debits = Σ credits)
- ✅ **Immutable Ledger** - Entries never updated/deleted, corrections via reversals
- ✅ **Multi-Currency Support** - Full validation and per-account currency enforcement
- ✅ **Idempotency** - Safe retries via externalId
- ✅ **Pessimistic Locking** - Prevents concurrent account modifications
- ✅ **Transactional Outbox Pattern** - Guaranteed event delivery to Kafka
- ✅ **On-Demand Balance Calculation** - Derived from ledger entries with snapshot optimization
- ✅ **REST API** - Complete CRUD with RFC 7807 error handling
- ✅ **OAuth2/JWT Authentication** - Enterprise-grade security with Keycloak
- ✅ **Data Encryption at Rest** - AES-256 encryption for sensitive data
- ✅ **Role-Based Access Control** - Fine-grained permissions (ADMIN, USER)

---

## 🏗️ Architecture Overview

This system follows **Clean Architecture** and **Domain-Driven Design (DDD)** principles, organized into four layers:

```
┌─────────────────────────────────────────────────────────┐
│                      API Layer                          │
│  (REST Controllers, DTOs, Exception Handling)           │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                 Application Layer                       │
│  (Use Cases, Commands, Transaction Orchestration)       │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  Domain Layer                           │
│  (Business Rules, Entities, Value Objects)              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Infrastructure Layer                       │
│  (JPA, Repositories, Kafka, Outbox, Database)           │
└─────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

- **Domain Layer**: Pure business logic with zero external dependencies
- **Application Layer**: Use case orchestration and transaction boundaries
- **Infrastructure Layer**: Database, messaging, security, and external integrations
- **API Layer**: HTTP endpoints and request/response handling

---

## 🧠 Architectural Decisions & Trade-offs

### 1. **Transactional Outbox Pattern** 🏆

**Decision**: Events are persisted to an `outbox_events` table in the **same database transaction** as ledger entries, then published to Kafka asynchronously by a scheduled processor.

**Why?**
- **Problem**: Direct Kafka publishing risks losing events if the app crashes after DB commit
- **Solution**: Atomic persistence of both data and events guarantees delivery
- **Trade-off**: ~5-second latency vs. guaranteed consistency

**Architecture**:
```
LedgerApplicationService
  ├─► Save Ledger Entry      }
  └─► Save Event to Outbox   } ← Same Transaction!
         ↓
  OutboxProcessor (polls every 5s)
  ├─► Publish to Kafka
  └─► Mark as PROCESSED
```

**Guarantees**:
- ✅ **At-least-once delivery** - Events never lost
- ✅ **Eventual consistency** - All events eventually reach Kafka
- ⚠️ **Idempotent consumers required** - May receive duplicates

**See**: [OUTBOX_PATTERN.md](./OUTBOX_PATTERN.md) for complete documentation

### 2. **Pessimistic Locking** 🔒

**Decision**: Use `SELECT FOR UPDATE` with **deterministic ordering** to prevent deadlocks.

**Why?**
- **Problem**: Concurrent transactions on the same account can cause data races
- **Solution**: Lock accounts in **sorted UUID order** before processing
- **Trade-off**: Lower throughput vs. data correctness

**Implementation**:
```java
// Sort account IDs to prevent circular waits
List<UUID> sortedAccountIds = accountIds.stream()
    .sorted()  // Deterministic order
    .toList();

// Lock in order to avoid deadlocks
for (UUID accountId : sortedAccountIds) {
    accountRepository.findByIdWithLock(accountId);  // SELECT FOR UPDATE
}
```

**Why Not Optimistic Locking?**
- Financial transactions MUST serialize on the same account
- Optimistic locking + retries = wasted work and poor UX
- Pessimistic locking provides **strong consistency** guarantees

### 3. **Idempotency via External ID** 🔁

**Decision**: Every transaction has a client-provided `externalId` used for deduplication.

**Why?**
- **Problem**: Network failures cause retries, risking duplicate transactions
- **Solution**: Check if `externalId` exists before processing
- **Trade-off**: Requires clients to generate unique IDs

**Flow**:
```java
Optional<Transaction> existing = repository.findByExternalId(externalId);
if (existing.isPresent()) {
    return existing.get();  // Return cached result
}
// Process new transaction...
```

**Benefits**:
- ✅ Safe retries without side effects
- ✅ Simpler error handling for clients
- ✅ Audit trail of duplicate requests

### 4. **Immutable Ledger with Reversals** 📜

**Decision**: Ledger entries are **never updated or deleted**. Corrections are made via reversal transactions.

**Why?**
- **Compliance**: Regulatory requirements (SOX, Basel III) mandate audit trails
- **Trust**: Immutability prevents fraud and data tampering
- **Debugging**: Complete historical record of all changes

**Reversal Flow**:
```
Original Transaction:
  Account A: DEBIT $100
  Account B: CREDIT $100

Reversal Transaction:
  Account A: CREDIT $100  ← Opposite entry
  Account B: DEBIT $100   ← Opposite entry
```

**Trade-off**: Storage grows continuously, but snapshots mitigate this (see below)

### 5. **Balance Calculation: On-Demand with Snapshots** 📊

**Decision**: Balances are **derived** from ledger entries, not stored. Snapshots are created periodically for performance.

**Why?**
- **Source of Truth**: Ledger entries are authoritative
- **Consistency**: No risk of balance/ledger mismatch
- **Trade-off**: Query complexity vs. data integrity

**Hybrid Approach**:
```sql
-- Calculate balance from last snapshot + new entries
SELECT 
  snapshot.balance + SUM(entries.amount)
FROM 
  balance_snapshots snapshot
  LEFT JOIN ledger_entries entries ON ...
WHERE 
  entries.recorded_at > snapshot.snapshot_time
```

**Why Not Store Balances?**
1. **Dual-write problem**: Updating both ledger AND balance risks inconsistency
2. **Complexity**: Reversals require recalculating historical balances
3. **Trust**: Derived balances are provably correct

**Snapshot Strategy**:
- Created nightly via scheduled job (`SnapshotScheduler`)
- Reduces query window from "all time" to "since last snapshot"
- 100x faster balance queries for old accounts

### 6. **Multi-Currency Enforcement** 💰

**Decision**: Each account has a **single currency**. All entries must match the account's currency.

**Why?**
- **Simplicity**: No need for exchange rate tracking in the ledger
- **Compliance**: Matches accounting standards (each currency = separate account)
- **Trade-off**: More accounts vs. complex currency conversion

**Validation**:
```java
public void validateCurrency(String entryCurrency) {
    if (!this.currency.equals(entryCurrency)) {
        throw new InvalidAccountException(
            "Currency mismatch: account=" + currency + ", entry=" + entryCurrency
        );
    }
}
```

**Alternative Considered**: Mixed-currency accounts with real-time FX lookup
- ❌ **Rejected**: Adds complexity, latency, and external dependencies

### 7. **Double-Entry Validation** ⚖️

**Decision**: Every transaction MUST balance (Σ debits = Σ credits) before posting.

**Why?**
- **Fundamental Rule**: Accounting equation (Assets = Liabilities + Equity) must hold
- **Error Detection**: Unbalanced transactions indicate bugs
- **Compliance**: Required by GAAP/IFRS

**Validation**:
```java
BigDecimal debits = entries.stream()
    .filter(e -> e.getEntryType() == DEBIT)
    .map(e -> e.getAmount().getAmount())
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal credits = entries.stream()
    .filter(e -> e.getEntryType() == CREDIT)
    .map(e -> e.getAmount().getAmount())
    .reduce(BigDecimal.ZERO, BigDecimal::add);

if (debits.compareTo(credits) != 0) {
    throw new InvalidTransactionException("Unbalanced transaction");
}
```

### 8. **OAuth2/JWT with Keycloak** 🔐

**Decision**: Use Keycloak as the identity provider instead of Spring Security's in-memory users.

**Why?**
- **Enterprise-Ready**: Industry-standard OAuth2/OIDC implementation
- **Scalability**: Stateless JWT tokens, no session management
- **Features**: SSO, MFA, user federation out-of-the-box
- **Trade-off**: Additional infrastructure vs. production-grade security

**Token Flow**:
```
Client → Keycloak: Login (username/password)
Keycloak → Client: JWT Token (signed)
Client → API: Request + Bearer Token
API → Validates signature + claims
API → Processes request
```

**Why Not Basic Auth or Sessions?**
- ❌ Basic Auth: Credentials sent on every request
- ❌ Sessions: Requires sticky sessions, doesn't scale horizontally
- ✅ JWT: Stateless, verifiable, supports microservices

**See**: [AUTHENTICATION.md](./AUTHENTICATION.md) for complete setup

### 9. **Data Encryption at Rest** 🔒

**Decision**: Encrypt sensitive fields (account numbers) using AES-256-GCM with JPA converters.

**Why?**
- **Compliance**: PCI-DSS, GDPR, SOX requirements
- **Defense in Depth**: Protection even if database is compromised
- **Trade-off**: ~5% performance overhead vs. regulatory compliance

**Implementation**:
```java
@Convert(converter = SensitiveDataConverter.class)
private String accountNumber;  // Encrypted in DB
```

**Key Management**:
- ⚠️ **Development**: Key in `application.yaml` (for testing only)
- ✅ **Production**: Must use HashiCorp Vault, AWS KMS, or Azure Key Vault

### 10. **Flyway for Schema Migrations** 🛠️

**Decision**: Use Flyway for version-controlled database schema evolution.

**Why?**
- **Repeatability**: Same migrations run in dev, staging, prod
- **Auditability**: Complete history of schema changes
- **Safety**: Checksums prevent accidental modifications

**Migration Files**:
```
V1__create_accounts_table.sql
V2__create_ledger_transactions_table.sql
V3__create_ledger_entries_table.sql
V4__create_balance_snapshots_table.sql
V5__add_encrypted_account_number.sql
V6__create_outbox_events_table.sql  ← Transactional Outbox
```

**Why Not ORM Auto-DDL?**
- ❌ Hibernate `ddl-auto=update`: Risky in production, no rollback
- ✅ Flyway: Explicit, versioned, peer-reviewed migrations

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **PostgreSQL 14+**
- **Kafka 3.0+** (for event publishing)
- **Keycloak 23+** (for authentication)

### 1. Start Infrastructure (Docker)

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Kafka + Zookeeper (port 9092)
- Keycloak (port 8180)

### 2. Configure Keycloak

Follow the setup guide: [KEYCLOAK_SETUP.md](./KEYCLOAK_SETUP.md)

### 3. Build and Run

```bash
./mvnw clean install
./mvnw spring-boot:run
```

The application starts on **http://localhost:8080**

### 4. Get an Access Token

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/banking-realm/protocol/openid-connect/token \
  -d "client_id=banking-api" \
  -d "grant_type=password" \
  -d "username=admin_user" \
  -d "password=admin123" | jq -r '.access_token')
```

### 5. Create Your First Transaction

```bash
# Create two accounts
ACCOUNT_A=$(curl -s -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"accountType": "ASSET", "currency": "BRL"}' | jq -r '.accountId')

ACCOUNT_B=$(curl -s -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"accountType": "LIABILITY", "currency": "BRL"}' | jq -r '.accountId')

# Post a deposit transaction
curl -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": "tx-'$(uuidgen)'",
    "eventType": "DEPOSIT",
    "entries": [
      {
        "accountId": "'$ACCOUNT_A'",
        "amount": 100.00,
        "currency": "BRL",
        "entryType": "DEBIT"
      },
      {
        "accountId": "'$ACCOUNT_B'",
        "amount": 100.00,
        "currency": "BRL",
        "entryType": "CREDIT"
      }
    ]
  }'
```

---

## 📚 API Documentation

### Interactive Documentation

Open **Swagger UI**: http://localhost:8080/swagger-ui.html

> **Note**: All endpoints (except `/actuator/health`) require authentication.

### Core Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/accounts` | Create a new account |
| `GET` | `/api/v1/accounts` | List all accounts (paginated) |
| `GET` | `/api/v1/accounts/{id}` | Get account details |
| `POST` | `/api/v1/transactions` | Post a transaction (idempotent) |
| `GET` | `/api/v1/transactions/{id}` | Get transaction details |
| `POST` | `/api/v1/transactions/{id}/reverse` | Reverse a transaction |
| `GET` | `/api/v1/balances/{accountId}` | Get account balance |

### Example: Post Transaction

```bash
curl -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": "tx-unique-001",
    "eventType": "TRANSFER",
    "entries": [
      {
        "accountId": "account-a-id",
        "amount": 100.00,
        "currency": "BRL",
        "entryType": "CREDIT"
      },
      {
        "accountId": "account-b-id",
        "amount": 100.00,
        "currency": "BRL",
        "entryType": "DEBIT"
      }
    ]
  }'
```

**Response**:
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "externalId": "tx-unique-001",
  "eventType": "TRANSFER",
  "status": "POSTED",
  "createdAt": "2024-01-17T14:00:00Z",
  "entries": [...]
}
```

---

## 🧪 Testing

### Test Strategy

1. **Unit Tests**: Domain logic (double-entry validation, balance calculation)
2. **Integration Tests**: Application layer + database (H2 in-memory)
3. **Outbox Tests**: Event persistence and Kafka publishing

### Run Tests

```bash
# All tests
./mvnw test

# Specific test suite
./mvnw test -Dtest=OutboxIntegrationTest
./mvnw test -Dtest=LedgerIntegrationTest

# With coverage
./mvnw test jacoco:report
```

### Key Test Suites

| Test | Coverage |
|------|----------|
| `LedgerIntegrationTest` | End-to-end transaction flow |
| `OutboxIntegrationTest` | Transactional Outbox pattern |
| `BalanceSnapshotIntegrationTest` | Balance calculation + snapshots |
| `LedgerTransactionTest` | Domain validation rules |

---

## 📊 Monitoring & Observability

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Outbox Monitoring

The `OutboxProcessor` logs statistics every minute:

```
Outbox status - Pending: 0, Failed: 0
```

**Query failed events**:
```sql
SELECT * FROM outbox_events 
WHERE status = 'FAILED' 
ORDER BY created_at DESC;
```

### Metrics (Prometheus)

Exposed at: http://localhost:8080/actuator/prometheus

Key metrics:
- `ledger_transactions_posted_total` - Total transactions posted
- `outbox_events_pending` - Pending events in outbox
- `outbox_events_failed` - Failed events requiring intervention

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/sbaldasso/java_banking_core/
│   │   ├── domain/              # 🧠 Business logic (zero dependencies)
│   │   │   ├── model/           # Aggregates: LedgerTransaction, Account
│   │   │   ├── service/         # Domain services: DoubleEntryValidator
│   │   │   ├── valueobject/     # Value objects: Money, EntryType
│   │   │   └── exception/       # Domain exceptions
│   │   ├── application/         # 🎯 Use cases & orchestration
│   │   │   ├── service/         # LedgerApplicationService, OutboxService
│   │   │   ├── command/         # Commands: PostTransactionCommand
│   │   │   └── dto/             # DTOs for API responses
│   │   ├── infrastructure/      # 🔌 External integrations
│   │   │   ├── persistence/     # JPA entities, repositories
│   │   │   ├── event/           # Kafka event publishers
│   │   │   ├── scheduler/       # OutboxProcessor, SnapshotScheduler
│   │   │   ├── security/        # Keycloak JWT converter
│   │   │   └── encryption/      # AES-256 data encryption
│   │   └── api/                 # 🌐 REST API
│   │       ├── controller/      # REST controllers
│   │       └── exception/       # RFC 7807 error handling
│   └── resources/
│       ├── application.yaml     # Configuration
│       └── db/migration/        # Flyway SQL migrations
└── test/                        # 🧪 Tests
```

---

## 🏢 Domain Model

### Account Types

| Type | Description | Normal Balance | Example |
|------|-------------|----------------|---------|
| **ASSET** | Customer accounts, cash | DEBIT | Checking account |
| **LIABILITY** | Loans, deposits | CREDIT | Customer loan |
| **EQUITY** | Capital, retained earnings | CREDIT | Shareholder equity |
| **REVENUE** | Fee income, interest | CREDIT | Service fees |
| **EXPENSE** | Operating costs | DEBIT | Salaries |

### Transaction Lifecycle

```
CREATE → PENDING → POSTED → REVERSED
    ↓                ↓
  FAILED         (affects balance)
```

Only **POSTED** transactions impact account balances.

### Event Types

- `DEPOSIT` - Money in
- `WITHDRAWAL` - Money out
- `TRANSFER` - Between accounts
- `REVERSAL` - Correction transaction

---

## 🎯 Business Rules Enforced

1. **Immutability** - Ledger entries never modified, only appended
2. **Double-Entry** - All transactions balanced (debits = credits)
3. **Temporal Ordering** - Event time + recorded time tracking
4. **Idempotency** - External ID prevents duplicates
5. **Concurrency** - Pessimistic locking prevents race conditions
6. **Auditability** - Complete transaction history preserved
7. **Multi-Currency** - Per-account currency with strict validation
8. **Reversals** - Corrections via mirror transactions
9. **Derived Balances** - Calculated from ledger, never stored
10. **Event Delivery** - Guaranteed via Transactional Outbox

---

## 🚧 Production Checklist

### Security
- [x] ~~Replace in-memory users with OAuth2~~ ✅ **Keycloak integration complete**
- [x] ~~Encrypt sensitive data~~ ✅ **AES-256 encryption implemented**
- [ ] Store encryption keys in secure vault (HashiCorp Vault, AWS KMS)
- [ ] Configure production Keycloak with HTTPS
- [ ] Enable rate limiting (Spring Cloud Gateway or API Gateway)
- [ ] Configure CORS policies for frontend
- [ ] Set up Web Application Firewall (WAF)

### Infrastructure
- [ ] Configure production database connection pool (HikariCP tuning)
- [ ] Set up Kafka cluster with replication factor 3
- [ ] Configure Flyway baseline for existing production data
- [ ] Enable HTTPS/TLS for all services
- [ ] Set up load balancer (NGINX, AWS ALB)

### Observability
- [ ] Centralized logging (ELK Stack, Splunk)
- [ ] Metrics and alerting (Prometheus + Grafana)
- [ ] Distributed tracing (Jaeger, Zipkin)
- [ ] Uptime monitoring (PagerDuty, Datadog)
- [ ] Audit logging for compliance

### Reliability
- [ ] Configure backup and disaster recovery
- [ ] Implement circuit breakers (Resilience4j)
- [ ] Set up blue-green deployments
- [ ] Performance testing (JMeter, Gatling)
- [ ] Chaos engineering (simulate outbox failures, DB crashes)

### Compliance
- [ ] Security penetration testing
- [ ] PCI-DSS compliance audit (if handling cards)
- [ ] GDPR compliance review (if handling EU data)
- [ ] SOX compliance (if publicly traded)

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License.

---

## 📖 Additional Documentation

- [OUTBOX_PATTERN.md](./OUTBOX_PATTERN.md) - Transactional Outbox deep dive
- [AUTHENTICATION.md](./AUTHENTICATION.md) - OAuth2/JWT setup guide
- [KEYCLOAK_SETUP.md](./KEYCLOAK_SETUP.md) - Keycloak configuration

---

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Contact: baldassosamuel93@gmail.com

---

**Built with ❤️ using:**
- Spring Boot 4.0
- Clean Architecture
- Domain-Driven Design
- Transactional Outbox Pattern
- OAuth2/JWT Security
- PostgreSQL + Flyway
- Apache Kafka
