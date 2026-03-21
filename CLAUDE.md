# STP Client — Claude Context

## Project
Cross-bank fund transfer client connecting to **STPG Hub (GWClientMU)** via SOAP.
Submits pacs.009 RTGS messages, polls inbound updates, and sends ACK/NAK.

**Stack:** Spring Boot 3.3.x · Java 21 · Spring-WS · JAXB · Oracle · Docker Swarm

---

## Package Layout
```
com.noh.stpclient
├── controller/     REST endpoints (/gw/*)
├── service/        Business logic (GwIntegrationService, SessionManager, GwPollingService)
├── remote/         SOAP client — GWClientMuRemote (extends WebServiceGatewaySupport)
├── audit/          AuditService, AuditRepository, AsyncAuditPersister
├── config/         SoapClientConfig, AsyncConfig, CustomSoapFaultMessageResolver
├── model/xml/      JAXB models (Logon, Send, GetUpdates, SendAckNak, DataPDU…)
├── utils/          CryptoManager (CMS + XAdES), SignatureManager
└── web/dto/        REST DTOs
```

---

## Core Functions

| Function | Entry Point | Key Steps |
|---|---|---|
| `logon` | `SessionManager.getOrCreate()` | CMS-sign password → SOAP Logon → cache sessionId |
| `send` | `performSendFinancialTransaction()` | Build pacs.009 → XAdES sign → SOAP Send → verify CMS resp |
| `getUpdate` | `performGetUpdates()` | SOAP GetUpdates → parse items → async audit |
| `setACKNAK` | `performSendAckNak()` | SOAP SendAckNak with type + MIR |
| `logout` | `SessionManager.logout()` | SOAP Logout → clear AtomicReference |

---

## Controller Logging Pattern

Every controller method must follow this exact log structure:

```java
log.info(">>> START {methodName} >>>");
log.info("> request body: {}", request);
long start = System.currentTimeMillis();
// ... method logic ...
log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
log.info("<<< END {methodName} request <<<");
```

Example for `logon`:
```java
log.info(">>> START logon >>>");
log.info("> request body: {}", request);
long start = System.currentTimeMillis();
// logic
log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
log.info("<<< END logon request <<<");
```

- Use `log.info` (not debug/warn) for all boundary logs
- Log request body immediately after START
- Capture `start = System.currentTimeMillis()` right after logging the request body
- Log final response with `duration_ms` immediately before END
- `{methodName}` must match the Java method name exactly
- `requestId` is injected automatically via MDC from `RequestContextFilter` — no manual MDC calls needed in controller

---

## Critical Patterns

### Session Management
- Single session reused across all requests (AtomicReference + ReentrantLock)
- **EW1** fault code = session expired → call `invalidate(sessionId)` then `getOrCreate()`
- `@PreDestroy` auto-logout on shutdown

### SOAP Send
- `block4` must be **wrapped in CDATA** — preserves signed XML through marshalling
- `DataPDU` XML built by `DataPDUTransformer` then signed with XAdES before sending
- Response is CMS-signed by gateway — verified via `CryptoManager.verifyResponseSignature()`

### Audit Two-Step
- `recordSendBefore()` → INSERT PENDING row (synchronous, before SOAP)
- `recordSendAfter()` → @Async UPDATE with response (non-blocking)
- SOAP payloads captured via `ThreadLocal` in `SoapPayloadLoggingInterceptor` — **read before @Async dispatch**

### Crypto
- **Logon:** CMS SHA256withRSA · password encoded UTF-16LE · BouncyCastle
- **Send:** XAdES signature appended to `AppHdr/Sgntr` element
- Keystore: PKCS12 at `key/LBBCLALABXXX.pfx`

---

## SOAP Fault Codes

| Code | Meaning | Action |
|---|---|---|
| EW1 | Session not found / closed | `invalidate()` + retry |
| EW2 | Authentication failure | Check credentials/cert |
| EW3 | Invalid message format | Validate pacs.009 XML |

---

## Build & Deploy
```bash
mvnw clean package -DskipTests     # build jar
./build.sh dev                      # build image + push to registry
docker stack deploy -c docker/docker-stack.yml stp   # deploy swarm
docker service logs stp_stp-client -f                # tail logs
```

## Config Files (Docker-managed)
- `docker/docker-stack.yml` — Linux swarm (production)
- `docker/config_props/application.yml` — mounted as Docker config
- Keystore mounted as Docker secret → `/usr/apps/key/LBBCLALABXXX.pfx`

## Gotchas
- Never put credentials in CLAUDE.md — they are in Docker secrets / application-dev.yml (gitignored)
- `block4` wrapping in CDATA is intentional — do NOT remove
- Polling disabled by default (`stp.polling.enabled=false`) — enable only in prod
- `performSendFinancialTransaction` (two-step audit) ≠ `performFinancialTransaction` (single-step) — both exist, prefer the two-step version
- `SoapPayloadLoggingInterceptor` uses ThreadLocal — must be read on calling thread before any @Async handoff
