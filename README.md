# Banking Ledger Core System

A production-ready banking ledger system implementing double-entry bookkeeping principles, designed for high-integrity financial transaction processing.

## 🎯 Features

- ✅ **Double-Entry Bookkeeping** - All transactions balanced (Σ debits = Σ credits)
- ✅ **Immutable Ledger** - Entries never updated/deleted, corrections via reversals
- ✅ **Multi-Currency Support** - Full validation and per-account currency enforcement
- ✅ **Idempotency** - Safe retries via externalId
- ✅ **Pessimistic Locking** - Prevents concurrent account modifications
- ✅ **Event Publishing** - Kafka events for transaction lifecycle
- ✅ **On-Demand Balance Calculation** - Derived from ledger entries
- ✅ **REST API** - Complete CRUD with RFC 7807 error handling
- ✅ **OAuth2/JWT Authentication** - Enterprise-grade security with Keycloak
- ✅ **Data Encryption at Rest** - AES-256 encryption for sensitive data
- ✅ **Role-Based Access Control** - Fine-grained permissions (ADMIN, USER)

## 🏗️ Architecture

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
│  (JPA, Repositories, Kafka, Database)                   │
└─────────────────────────────────────────────────────────┘
```

### Clean Architecture Principles

- **Domain Layer**: Pure business logic, no dependencies
- **Application Layer**: Use case orchestration, transaction boundaries
- **Infrastructure Layer**: Database, messaging, external systems
- **API Layer**: HTTP endpoints, request/response handling

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **PostgreSQL 14+**
- **Kafka 3.0+** (optional, for event publishing)

### 1. Clone the Repository

```bash
git clone <repository-url>
cd java-banking-core
```

### 2. Configure Database

Edit `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ledger_db
    username: your_username
    password: your_password
```

Create the database:

```sql
CREATE DATABASE ledger_db;
```

### 3. Configure Kafka (Optional)

If you don't have Kafka, you can disable event publishing by commenting out the Kafka configuration in `application.yaml`.

### 4. Build the Project

```bash
./mvnw clean install
```

### 5. Run the Application

```bash
./mvnw spring-boot:run
```

The application starts on http://localhost:8080

## 🔐 Authentication

The API uses **OAuth2 with JWT tokens** powered by **Keycloak** for enterprise-grade authentication.

### Quick Start

1. **Start Keycloak:**
   ```bash
   docker-compose up keycloak keycloak-db
   ```
   Access Keycloak Admin Console at http://localhost:8180 (admin/admin)

2. **Configure Keycloak:**
   Follow the detailed setup guide in [KEYCLOAK_SETUP.md](./KEYCLOAK_SETUP.md)

3. **Get an Access Token:**
   ```bash
   curl -X POST http://localhost:8180/realms/banking-realm/protocol/openid-connect/token \
     -d "client_id=banking-api" \
     -d "grant_type=password" \
     -d "username=admin_user" \
     -d "password=admin123"
   ```

4. **Use the Token:**
   ```bash
   TOKEN="your_jwt_token_here"
   curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/accounts
   ```

### Available Roles

| Role | Permissions |
|------|-------------|
| **ADMIN** | Full access - create accounts, manage transactions, administrative operations |
| **USER** | Read/write access - view accounts, create transactions, query balances |

### Complete Authentication Guide

For detailed authentication flows, token management, and troubleshooting, see [AUTHENTICATION.md](./AUTHENTICATION.md)

### Security Features

- ✅ **OAuth2/OIDC** - Industry standard authentication protocol
- ✅ **JWT Tokens** - Stateless, cryptographically signed tokens
- ✅ **Role-Based Access Control** - Fine-grained permissions
- ✅ **Data Encryption at Rest** - AES-256 encryption for sensitive data
- ✅ **Keycloak Integration** - Enterprise identity and access management


## 📚 API Documentation

> **Note**: All endpoints (except `/actuator/health`) require a valid JWT token in the `Authorization: Bearer <TOKEN>` header.

### Create an Account

```bash
curl -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountType": "ASSET",
    "currency": "BRL"
  }'
```

**Response:**
```json
{
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "accountType": "ASSET",
  "currency": "BRL",
  "status": "ACTIVE",
  "createdAt": "2024-01-17T14:00:00Z"
}
```

### Post a Transaction

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

### Get Account Balance

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/balances/{accountId}
```

### Reverse a Transaction

```bash
curl -H "Authorization: Bearer $TOKEN" \
  -X POST http://localhost:8080/api/v1/transactions/{transactionId}/reverse \
  -H "Content-Type: application/json" \
  -d '{
    "reversalExternalId": "reversal-unique-001"
  }'
```

## 🧪 Testing

### Run All Tests

```bash
./mvnw test
```

### Run Specific Test Suite

```bash
# Domain tests
./mvnw test -Dtest=LedgerTransactionTest

# Integration tests
./mvnw test -Dtest=LedgerIntegrationTest
```

### Test Coverage

- **Domain Layer**: Double-entry validation, balance calculation, reversals
- **Application Layer**: Idempotency, transaction orchestration
- **Integration**: End-to-end flows with in-memory H2 database

## 📊 Database Schema

The system uses **Flyway** for database migrations:

- `V1__create_accounts_table.sql` - Creates accounts table
- `V2__create_ledger_transactions_table.sql` - Creates transactions table
- `V3__create_ledger_entries_table.sql` - Creates ledger entries table

Migrations run automatically on application startup.

## 🔧 Configuration

### Application Properties

Key configuration in `application.yaml`:

```yaml
# Database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ledger_db

# Kafka Topics
ledger:
  kafka:
    topics:
      transaction-posted: ledger.transactions.posted
      transaction-reversed: ledger.transactions.reversed

# Security (configure in production)
# JWT secret, OAuth2 providers, etc.
```

## 🏢 Domain Model

### Account Types

| Type      | Description                    | Normal Balance |
|-----------|--------------------------------|----------------|
| ASSET     | Customer accounts, cash        | DEBIT          |
| LIABILITY | Loans, obligations             | CREDIT         |
| EQUITY    | Capital, retained earnings     | CREDIT         |
| REVENUE   | Fee income, interest           | CREDIT         |
| EXPENSE   | Operating costs                | DEBIT          |

### Transaction Lifecycle

```
PENDING → POSTED → REVERSED
    ↓
  FAILED
```

Only **POSTED** transactions impact account balances.

## 🎯 Business Rules Enforced

1. **Imutabilidade** - Ledger entries never modified, only appended
2. **Partidas Dobradas** - All transactions balanced (debits = credits)
3. **Ordem Temporal** - Event time + recorded time tracking
4. **Idempotência** - External ID prevents duplicates
5. **Concorrência** - Pessimistic locking on accounts
6. **Auditoria** - Complete transaction history
7. **Multimoeda** - Per-account currency with validation
8. **Estorno** - Reversal creates mirror transaction
9. **Saldo Derivado** - Balance calculated from entries

## 🚧 Production Checklist

Before deploying to production:

- [x] ~~Replace in-memory users with database/OAuth2 authentication~~ ✅ **Implemented OAuth2/JWT with Keycloak**
- [x] ~~Implement data encryption at rest~~ ✅ **AES-256 encryption for sensitive data**
- [ ] Configure production Keycloak instance with HTTPS
- [ ] Store encryption keys in secure vault (HashiCorp Vault, AWS KMS, etc.)
- [ ] Configure production database connection pool
- [ ] Set up Kafka cluster and topics
- [ ] Add API rate limiting
- [ ] Configure CORS policies
- [ ] Set up monitoring and alerting
- [ ] Enable HTTPS/TLS for all services
- [ ] Configure centralized logging
- [ ] Add OpenAPI/Swagger documentation
- [ ] Implement comprehensive audit logging
- [ ] Set up CI/CD pipeline
- [ ] Configure backup and disaster recovery
- [ ] Perform security penetration testing
- [ ] Configure token rotation policies

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/sbaldasso/java_banking_core/
│   │   ├── domain/              # Business logic
│   │   │   ├── model/           # Entities, aggregates
│   │   │   ├── service/         # Domain services
│   │   │   ├── valueobject/     # Value objects, enums
│   │   │   └── exception/       # Domain exceptions
│   │   ├── application/         # Use cases
│   │   │   ├── service/         # Application services
│   │   │   ├── command/         # Commands
│   │   │   └── dto/             # DTOs
│   │   ├── infrastructure/      # External concerns
│   │   │   ├── persistence/     # JPA, repositories
│   │   │   ├── event/           # Kafka publishers
│   │   │   └── security/        # Spring Security
│   │   └── api/                 # REST API
│   │       ├── controller/      # Controllers
│   │       └── exception/       # Exception handlers
│   └── resources/
│       ├── application.yaml     # Configuration
│       └── db/migration/        # Flyway migrations
└── test/                        # Tests
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License.

## 🐛 Known Issues

- Integration tests require H2 in-memory database
- Kafka is optional but recommended for production
- Keycloak requires additional setup (see KEYCLOAK_SETUP.md)
- Encryption key should be stored in a secure vault for production

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Contact: baldassosamuel93@gmail.com

---

**Built with ❤️ using Spring Boot, Clean Architecture, and Domain-Driven Design**
