# GWClientMU Spring Boot SOAP Client

Spring Boot REST → SOAP bridge for `tmsxuat.bolnet.gov.la` — **GWClientMUService**.

---

## Architecture

```
HTTP Client
    │
    ▼
GwClientController   (REST Layer   — /api/gwclient/*)
    │
    ▼
GwClientService      (Business Layer — validation, session lifecycle)
    │
    ▼
GwClientSoapClient   (Transport Layer — extends WebServiceGatewaySupport)
    │
    ▼
TMS XUAT SOAP API    (http://tmsxuat.bolnet.gov.la:7080/GWClientMUService/GWClientMU)
```

---

## 5 Endpoints

| # | Operation     | HTTP Method | REST Path                       | Description                          |
|---|---------------|-------------|---------------------------------|--------------------------------------|
| 1 | `logon`       | POST        | `/api/gwclient/logon`           | Authenticate, returns `session_id`   |
| 2 | `logout`      | POST        | `/api/gwclient/logout`          | Invalidate session                   |
| 3 | `send`        | POST        | `/api/gwclient/send`            | Send a SWIFT MT message              |
| 4 | `getUpdates`  | GET         | `/api/gwclient/updates`         | Poll for inbound messages            |
| 5 | `sendACKNAK`  | POST        | `/api/gwclient/sendACKNAK`      | Send ACK or NAK for received message |

> Each endpoint also has a `/session` variant that accepts `X-Session-Id` header for stateful session reuse.

---

## Session Modes

### Mode A — Stateless (default, simpler)
Each REST call handles its own logon → operation → logout automatically.
```
POST /api/gwclient/send          { body }   → logon + send + logout in one call
GET  /api/gwclient/updates                  → logon + getUpdates + logout
POST /api/gwclient/sendACKNAK    { body }   → logon + sendACKNAK + logout
```

### Mode B — Stateful (better for batching)
Caller manages the session explicitly.
```
1. POST /api/gwclient/logon               → { "session_id": "abc123" }
2. POST /api/gwclient/send/session        Header: X-Session-Id: abc123
3. GET  /api/gwclient/updates/session     Header: X-Session-Id: abc123
4. POST /api/gwclient/sendACKNAK/session  Header: X-Session-Id: abc123
5. POST /api/gwclient/logout              Header: X-Session-Id: abc123
```

---

## Setup

### 1. Environment Variables

```bash
export GWCLIENT_SOAP_URL=http://tmsxuat.bolnet.gov.la:7080/GWClientMUService/GWClientMU
export GWCLIENT_USERNAME=SENDER22XXXX
export GWCLIENT_PASSWORD=your_password
```

### 2. Generate JAXB classes from WSDL

```bash
mvn clean generate-sources
```
Classes are generated to `target/generated-sources/jaxb/com/example/gwclient/generated/`.

### 3. Run

```bash
mvn spring-boot:run
```

---

## Example API Calls

### Logon
```bash
curl -X POST http://localhost:8080/api/gwclient/logon
# Response: { "session_id": "abc123xyz" }
```

### Send SWIFT MT103
```bash
curl -X POST http://localhost:8080/api/gwclient/send \
  -H "Content-Type: application/json" \
  -d '{
    "block4": ":20:REF123\n:32A:240501USD1000,\n:50K:SENDER NAME\n:59:RECEIVER NAME\n:71A:SHA",
    "msgSender": "SENDER22XXXX",
    "msgReceiver": "BANKLAXXXXX",
    "msgType": "103",
    "format": "MT"
  }'
```

### Get Updates
```bash
curl http://localhost:8080/api/gwclient/updates
```

### Send ACK
```bash
curl -X POST http://localhost:8080/api/gwclient/sendACKNAK \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ACK",
    "datetime": "2024-05-01T10:00:00",
    "mir": "2405011000SENDER22XXXX0001"
  }'
```

### Logout (stateful)
```bash
curl -X POST http://localhost:8080/api/gwclient/logout \
  -H "X-Session-Id: abc123xyz"
```

---

## Project Structure

```
src/main/
├── java/com/example/gwclient/
│   ├── GwClientApplication.java
│   ├── config/
│   │   └── WebServiceConfig.java         # Jaxb2Marshaller + SoapClient bean
│   ├── client/
│   │   └── GwClientSoapClient.java       # All 5 SOAP operations (transport only)
│   ├── service/
│   │   └── GwClientService.java          # Business logic + session management
│   ├── controller/
│   │   └── GwClientController.java       # REST endpoints
│   ├── model/request/
│   │   ├── SendMessageRequest.java
│   │   └── SendACKNAKRequest.java
│   └── exception/
│       ├── GwClientSoapException.java
│       └── GlobalExceptionHandler.java
└── resources/
    ├── wsdl/
    │   └── GWClientMU.wsdl               # WSDL for JAXB class generation
    └── application.yml
```