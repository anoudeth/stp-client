---
name: soap-debug
description: Debug SOAP faults and errors from STPG Hub (GWClientMU). Use when investigating EW1/EW2/EW3 fault codes, SOAP payload issues, session expiry, or gateway response problems.
allowed tools: Read, Grep, Bash
---

# SOAP Debug Guide — STPG Hub

## 1. Find the Fault Code
```bash
# Grep logs for SOAP fault detail
grep -n "FaultDetail\|GatewayIntegrationException\|EW1\|EW2\|EW3" logs/stp-client.log | tail -30
```

## 2. Fault Code Reference
| Code | Meaning | Fix |
|---|---|---|
| EW1 | Session not found / expired | Call `invalidate(sessionId)` then `getOrCreate()` — already handled in service |
| EW2 | Authentication failure | Check username/password in application-dev.yml; verify keystore alias |
| EW3 | Invalid message format | Validate pacs.009 XML structure; check CDATA wrapping of block4 |

## 3. Session Expiry Flow
Check `SessionManager.java`:
- `isSessionExpired(e)` checks for EW1 in `SESSION_EXPIRED_CODES`
- `invalidate(sessionId)` uses `compareAndSet` — thread-safe
- `getOrCreate()` uses double-checked locking with `ReentrantLock`

## 4. SOAP Payload Inspection
The `SoapPayloadLoggingInterceptor` stores payloads in ThreadLocal:
```java
// Stored in: SoapPayloadLoggingInterceptor → ThreadLocal
// Persisted to: STP_AUDIT_LOG.SOAP_REQUEST / SOAP_RESPONSE (CLOB)
```
Query Oracle to inspect:
```sql
SELECT SOAP_REQUEST, SOAP_RESPONSE, ERROR_CODE, ERROR_MESSAGE
FROM STP_AUDIT_LOG
WHERE CREATED_AT > SYSDATE - 1/24
ORDER BY CREATED_AT DESC;
```

## 5. CustomSoapFaultMessageResolver
Located at `config/CustomSoapFaultMessageResolver.java`:
- Extracts `FaultDetail` XML from SOAP response
- Throws `GatewayIntegrationException` with code + description + info
- Check logs for: `GatewayIntegrationException` with `code=EW*`

## 6. CDATA Issue
If gateway rejects the message with XML parse error:
- Verify `block4` is wrapped in CDATA in `GWClientMuRemote.send()`
- Signed XML must NOT be re-encoded during marshalling

## 7. Signature Verification Warning
`performSend` logs a warning (not error) if gateway signature fails:
```
Gateway response signature verification FAILED for session: xxx
```
- Check `gw.cert.alias` in application-dev.yml matches the imported gateway cert
- Use: `keytool -list -keystore key/LBBCLALABXXX.pfx -storetype PKCS12`
