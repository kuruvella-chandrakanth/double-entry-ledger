# Double-Entry Ledger Service

A production-ready double-entry accounting ledger built with **Java 17**, **Spring Boot 3**, and **MySQL**.

---

## Design Principles

### Why double-entry?
Every money movement creates **two** ledger entries — a DEBIT and a CREDIT of equal amount on different accounts. This means money can never appear or disappear: it only moves. The invariant `sum(all DEBITs) == sum(all CREDITs)` must hold at all times and is provable directly from the data.

### Key guarantees

| Property | How it's enforced |
|---|---|
| **Invariant** | `sum(DEBIT) == sum(CREDIT)` validated before write; provable via `GET /api/ledger/verify` |
| **Idempotency** | `idempotency_key` UNIQUE constraint in DB — retried requests return the original result |
| **Concurrency** | `SERIALIZABLE` isolation + `SELECT … FOR UPDATE` on accounts in ID order (prevents deadlocks) |
| **Immutability** | Ledger entries are append-only; no UPDATE/DELETE in application code |
| **Precision** | Amounts stored as `BIGINT` minor units (cents) — no floating-point arithmetic |

### Data model

```
accounts
  id, name, account_type (ASSET|LIABILITY|EQUITY|REVENUE|EXPENSE), currency, created_at

ledger_transactions
  id, idempotency_key (UNIQUE), description, status, created_at

ledger_entries
  id, transaction_id → ledger_transactions, account_id → accounts,
  entry_type (DEBIT|CREDIT), amount (minor units, > 0), currency, created_at
```

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0+ (or use Docker below)

### 1. Start MySQL with Docker

```bash
docker-compose up -d
```

This starts MySQL on port 3306 with the `ledger_db` database pre-created.

### 2. Run the application

```bash
mvn spring-boot:run
```

Flyway will automatically run `V1__init_ledger_schema.sql` on first start.

The service starts on `http://localhost:8080`.

---

## API Reference

### Accounts

#### Create an account
```http
POST /api/accounts
Content-Type: application/json

{
  "name": "User Wallet",
  "accountType": "ASSET",
  "currency": "USD"
}
```

#### Get account balance
```http
GET /api/accounts/{id}/balance
```

Response:
```json
{
  "accountId": 1,
  "name": "User Wallet",
  "accountType": "ASSET",
  "currency": "USD",
  "totalDebits": 10000,
  "totalCredits": 3000,
  "balance": 7000
}
```
> All amounts are in **minor units** (e.g. `7000` = $70.00 USD).

#### Get account statement (bank-style)
```http
GET /api/accounts/{id}/statement?page=0&size=50
```

Response:
```json
{
  "accountId": 1,
  "accountName": "User Wallet",
  "currency": "USD",
  "currentBalance": 7000,
  "totalEntries": 3,
  "entries": [
    {
      "entryId": 1,
      "transactionId": 1,
      "entryType": "DEBIT",
      "amount": 10000,
      "runningBalance": 10000,
      "description": "Initial top-up",
      "createdAt": "2024-01-01T10:00:00Z"
    },
    {
      "entryId": 3,
      "transactionId": 2,
      "entryType": "CREDIT",
      "amount": 3000,
      "runningBalance": 7000,
      "description": "Purchase at merchant",
      "createdAt": "2024-01-02T14:30:00Z"
    }
  ]
}
```

---

### Transactions

#### Post a transaction
```http
POST /api/transactions
Content-Type: application/json

{
  "idempotencyKey": "3f4e5d6c-7a8b-4c2d-9e1f-a0b1c2d3e4f5",
  "description": "User wallet top-up",
  "entries": [
    { "accountId": 1, "entryType": "DEBIT",  "amount": 5000 },
    { "accountId": 2, "entryType": "CREDIT", "amount": 5000 }
  ]
}
```

Rules:
- `sum(DEBIT amounts)` **must** equal `sum(CREDIT amounts)` → HTTP 422 if not
- `idempotencyKey` must be unique per logical operation — retrying with the same key is safe
- At least two entries are required

#### Get a transaction
```http
GET /api/transactions/{id}
```

---

### Ledger

#### Prove the invariant
```http
GET /api/ledger/verify
```

Response:
```json
{
  "totalDebits": 150000,
  "totalCredits": 150000,
  "difference": 0,
  "balanced": true,
  "checkedAt": "2024-01-15T09:00:00Z"
}
```

This is a **live proof** computed directly from `ledger_entries` — not an assertion.

---

## Error Responses

All errors follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457):

```json
{
  "type": "/errors/imbalanced-transaction",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Transaction is imbalanced: total debits=5000, total credits=3000 (difference=2000)."
}
```

| Scenario | HTTP Status |
|---|---|
| Account not found | 404 |
| Transaction not found | 404 |
| Imbalanced entries | 422 |
| Validation failure (missing fields, negative amount) | 400 |
| Server error | 500 (no internal detail leaked) |

---

## Walkthrough: Moving money

```bash
# 1. Create a "Cash Pool" liability account (the source of funds)
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"Cash Pool","accountType":"LIABILITY","currency":"USD"}' | jq .

# 2. Create a "User Wallet" asset account
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"User Wallet","accountType":"ASSET","currency":"USD"}' | jq .

# 3. Top up the wallet: DEBIT wallet (asset +), CREDIT cash pool (liability +)
curl -s -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "topup-001",
    "description": "User wallet top-up $100",
    "entries": [
      {"accountId": 2, "entryType": "DEBIT",  "amount": 10000},
      {"accountId": 1, "entryType": "CREDIT", "amount": 10000}
    ]
  }' | jq .

# 4. Check wallet balance — should be $100.00
curl -s http://localhost:8080/api/accounts/2/balance | jq .

# 5. Prove the invariant holds
curl -s http://localhost:8080/api/ledger/verify | jq .

# 6. Retry the same top-up with the same idempotency key — money moves once only
curl -s -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "topup-001",
    "description": "User wallet top-up $100",
    "entries": [
      {"accountId": 2, "entryType": "DEBIT",  "amount": 10000},
      {"accountId": 1, "entryType": "CREDIT", "amount": 10000}
    ]
  }' | jq .
# Returns the original transaction — balance is still $100.00
```

---

## Running Tests

```bash
mvn test
```
