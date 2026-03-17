---
name: audit-trace
description: Trace a financial transaction through STP_AUDIT_LOG and STP_SEND_TXN_DETAIL tables. Use when investigating a transfer status, finding a transaction by MIR/MSG_ID, or checking audit records.
allowed tools: Read, Bash
---

# Audit Trace — STP Oracle Tables

## Tables
- `STP_AUDIT_LOG` — core audit entry (all operations: SEND, GET_UPDATES, SEND_ACK_NAK)
- `STP_SEND_TXN_DETAIL` — transaction detail (FK: AUDIT_LOG_ID → STP_AUDIT_LOG.ID)

## Find by MIR (Message Input Reference)
```sql
SELECT
    a.ID, a.OPERATION, a.STATUS, a.CREATED_AT,
    a.ERROR_CODE, a.ERROR_MESSAGE,
    d.RESPONSE_MIR, d.RESPONSE_TYPE, d.RES_CODE, d.RES_MESSAGE,
    d.AMOUNT, d.CURRENCY, d.SETTLEMENT_DATE,
    d.DEBTOR_NAME, d.CREDITOR_NAME, d.MSG_ID
FROM STP_AUDIT_LOG a
JOIN STP_SEND_TXN_DETAIL d ON d.AUDIT_LOG_ID = a.ID
WHERE d.RESPONSE_MIR = :mir
ORDER BY a.CREATED_AT DESC;
```

## Find by MSG_ID
```sql
SELECT * FROM STP_SEND_TXN_DETAIL
WHERE MSG_ID = :msgId OR BUSINESS_MSG_ID = :msgId;
```

## Find Recent Failures
```sql
SELECT a.ID, a.OPERATION, a.STATUS, a.ERROR_CODE, a.ERROR_MESSAGE,
       a.CREATED_AT, d.MSG_ID, d.AMOUNT, d.CURRENCY
FROM STP_AUDIT_LOG a
LEFT JOIN STP_SEND_TXN_DETAIL d ON d.AUDIT_LOG_ID = a.ID
WHERE a.STATUS = 'FAILURE'
  AND a.CREATED_AT > SYSDATE - 1
ORDER BY a.CREATED_AT DESC;
```

## Check PENDING (stuck transactions)
```sql
SELECT a.ID, a.OPERATION, a.CREATED_AT, d.MSG_ID, d.AMOUNT
FROM STP_AUDIT_LOG a
LEFT JOIN STP_SEND_TXN_DETAIL d ON d.AUDIT_LOG_ID = a.ID
WHERE a.STATUS = 'PENDING'
  AND a.CREATED_AT < SYSDATE - 1/24   -- older than 1 hour
ORDER BY a.CREATED_AT;
```

## Inspect Full SOAP Payload (CLOB)
```sql
SELECT DBMS_LOB.SUBSTR(SOAP_REQUEST, 4000, 1) AS SOAP_REQ,
       DBMS_LOB.SUBSTR(SOAP_RESPONSE, 4000, 1) AS SOAP_RESP
FROM STP_AUDIT_LOG
WHERE ID = :auditLogId;
```

## Audit Flow Reference
1. `recordSendBefore()` → INSERT PENDING + request fields into both tables
2. SOAP call executes
3. `recordSendAfter()` → @Async UPDATE STATUS + response fields (RES_CODE, RESPONSE_MIR, etc.)

**Note:** A PENDING row with no update = async persister may have failed. Check app logs for `AsyncAuditPersister` errors.

## Key Columns in STP_SEND_TXN_DETAIL
| Column | Description |
|---|---|
| RESPONSE_MIR | SWIFT Message Input Reference from gateway ACK |
| RESPONSE_TYPE | ACK or NAK |
| RES_CODE | Gateway response code |
| MSG_ID | pacs.009 business message ID |
| SETTLEMENT_DATE | Value date (YYYY-MM-DD) |
| SENDER_BIC / RECEIVER_BIC | BIC codes |
| AMOUNT / CURRENCY | Transfer amount |
