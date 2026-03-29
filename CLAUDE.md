# STP Client — Claude Context

## Project
Cross-bank fund transfer client connecting to **STPG Hub (GWClientMU)** via SOAP.
Polls inbound updates, and sends ACK/NAK.

**Stack:** Spring Boot 3.3.x · Java 21 · Spring-WS · JAXB · Oracle · Docker Swarm

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


---
### 1. Plan Mode Default
- Enter plan mode for ANY not-trivial task (3+ steps or architectural decisions)
- Use plan mode for verification steps, not just building
- Write detailed specs upfront to reduce ambiguity

### 2. Self-Improvement Loop
- After ANY correction from the user: update `tasks/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until the mistake rate drops
- Review lessons at session start for a project

### 3. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness

### 4. Demand Elegance (Balanced)
- For non-trivial changes: pause and ask "is there a more elegant way?"
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution"
- Skip this for simple, obvious fixes. Don't overengineer
- Challenge your own work before presenting it

### 5. Skills usage
- Use skills for any task that requires a capability
- Load skills from `.claude/skills/`
- Invoke skills with natural language
- Each skill is one independent capability

### 6. Subagents usage
- Use subagents liberally to keep the main context window clean
- Load subagents from `.claude/agents/`
- For complex problems, throw more compute at it via subagents
- One task per subagent for focused execution on a given tech stack

## Core Principles
- **Simplicity First**: Make every change as simple as possible. Impact minimal code
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards

## Project General Instructions

- Always use the latest versions of dependencies.
- Always write Java code as the Spring Boot application.
- Always use Maven for dependency management.
- Always create test cases for the generated code both positive and negative.
- Always generate the CircleCI pipeline in the .circleci directory to verify the code.
- Minimize the amount of code generated.
- The Maven artifact name must be the same as the parent directory name.
- Use semantic versioning for the Maven project. Each time you generate a new version, bump the PATCH section of the version number.
- Use `pl.piomin.services` as the group ID for the Maven project and base Java package.
- Do not use the Lombok library.
- Generate the Docker Compose file to run all components used by the application.
- Update README.md each time you generate a new version.
