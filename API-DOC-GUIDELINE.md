# API Documentation Review Guideline
## STP Client — SWIFT Gateway Integration API

---

## 1. Accessing the API Documentation

Start the application, then open:

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:7003/stp-client/swagger-ui.html |
| OpenAPI JSON | http://localhost:7003/stp-client/api-docs |
| OpenAPI YAML | http://localhost:7003/stp-client/api-docs.yaml |

> **Note:** `tryItOutEnabled: true` is configured, so you can test endpoints directly from Swagger UI.

---

## 2. Endpoints to Review

All endpoints are under the **`Gateway Integration`** tag. Verify all 6 are visible:

| Method | Path | Purpose |
|---|---|---|
| POST | `/gw/logon` | Authenticate with SWIFT gateway |
| POST | `/gw/logout` | Terminate gateway session |
| POST | `/gw/get-updates` | Poll for inbound SWIFT messages |
| POST | `/gw/send` | Submit a financial transaction |
| POST | `/gw/send-ack-nak` | Send ACK or NAK for a received message |
| POST | `/gw/financial-transaction` | End-to-end transaction (full pipeline) |

---

## 3. Request Envelope Structure

All requests use the generic wrapper `ApiRequest<T>`:

```json
{
  "requestId": "req-20260314-001",
  "requestDt": "2026-03-14T10:00:00",
  "data": { ... }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `requestId` | String | Yes | Unique request identifier |
| `requestDt` | String | Yes | Request datetime (ISO format recommended) |
| `data` | Object | Varies per endpoint | Endpoint-specific payload |

> **Check:** Verify that `requestId` and `requestDt` appear in Swagger as required fields with `@NotBlank` validation.

---

## 4. Response Envelope Structure

All responses use `ApiResponse<T>` wrapping `BaseResponse<T>`:

```json
{
  "resCode": "0000",
  "resMessage": "Success",
  "resStatus": "OK",
  "requestId": "req-20260314-001",
  "resId": "res-20260314-001",
  "resTimestamp": "2026-03-14T10:00:01Z",
  "processingTime": 120,
  "data": { ... },
  "error": null
}
```

| Field | Type | Description |
|---|---|---|
| `resStatus` | Enum | `OK`, `FAILED`, `PENDING`, `PARTIAL_SUCCESS` |
| `resCode` | String | Application-level result code |
| `resMessage` | String | Human-readable message |
| `data` | Object | Endpoint-specific response payload |
| `error` | Object | Populated on failure (see §7) |

> **Important:** HTTP 200 does NOT always mean success. Always check `resStatus` in the body.

---

## 5. Per-Endpoint Checklist

### POST `/gw/logon`

**Request body — `data` is `LogonRequest`:**
```json
{
  "requestId": "req-001",
  "requestDt": "2026-03-14T10:00:00",
  "data": {
    "username": "LBBCLALABXXX",
    "password": "••••••••"
  }
}
```

| Field | Validation | Schema annotation |
|---|---|---|
| `username` | `@NotBlank` | example: `LBBCLALABXXX` |
| `password` | `@NotBlank` | masked in toString |

**Check responses:**
- `200` — Logon success: `data.sessionId` is populated
- `400` — `data` field is null

---

### POST `/gw/logout`

**Request body — `data` is `LogoutRequest`:**

> Check that `LogoutRequest` fields are documented with `@Schema`.

**Check responses:**
- `200` — Logout success: `data` is null
- `400` — `data` field is null

---

### POST `/gw/get-updates`

**Request body — `data` is `Void` (no payload needed):**
```json
{
  "requestId": "req-002",
  "requestDt": "2026-03-14T10:00:00",
  "data": null
}
```

**Check responses:**
- `200` — `data` is a list of `GetUpdatesResponseDto` (may be empty if no pending messages)

> Verify `GetUpdatesResponseDto` fields are documented with `@Schema`.

---

### POST `/gw/send`

**Request body — `data` is `FinancialTransactionRequest`:**
```json
{
  "requestId": "req-003",
  "requestDt": "2026-03-14T10:00:00",
  "data": {
    "transaction": {
      "messageId": "MSG20260314001",
      "msgSequence": "0000001",
      "businessMessageId": "BIZID-20260314-001",
      "senderBic": "LBBCLALABXXX",
      "receiverBic": "LPDRLALAXATS",
      "instructingAgentBic": "LBBCLALABXXX",
      "instructedAgentBic": "LPDRLALAXATS",
      "debtorAgentBic": "LBBCLALABXXX",
      "currency": "LAK",
      "amount": 1000000.00,
      "settlementDate": "2026-03-14",
      "debtorName": "Lao Brewery Co., Ltd.",
      "debtorAccount": "0100001234567",
      "debtorAgentAccount": "0199990000001",
      "creditorName": "Phongsavanh Bank",
      "creditorAccount": "0200007654321",
      "creditorAgentAccount": "0299990000002",
      "debtorAddressLines": ["123 Lane Xang Avenue", "Vientiane, Lao PDR"],
      "creditorAddressLines": ["456 Samsenthai Road", "Vientiane, Lao PDR"],
      "instrForNxtAgt": "/RETN/",
      "remittanceInformation": "Payment for invoice INV-2026-0042"
    }
  }
}
```

> **Note:** `sessionId` is `hidden: true` in Swagger — it is managed internally by the server.

**Field validation rules for `TransactionData`:**

| Field | Required | Validation |
|---|---|---|
| `messageId` | Yes | `@NotBlank` |
| `msgSequence` | Yes | `@NotBlank` |
| `businessMessageId` | Yes | `@NotBlank` |
| `senderBic` | Yes | `@NotBlank` |
| `receiverBic` | Yes | `@NotBlank` |
| `instructingAgentBic` | Yes | `@NotBlank` |
| `instructedAgentBic` | Yes | `@NotBlank` |
| `debtorAgentBic` | Yes | `@NotBlank` |
| `currency` | Yes | `@NotBlank` |
| `amount` | Yes | `@NotNull`, `@Positive` |
| `settlementDate` | Yes | `@NotNull` |
| `debtorName` | Yes | `@NotBlank` |
| `debtorAccount` | Yes | `@NotBlank` |
| `debtorAgentAccount` | Yes | `@NotBlank` |
| `creditorName` | Yes | `@NotBlank` |
| `creditorAccount` | Yes | `@NotBlank` |
| `creditorAgentAccount` | Yes | `@NotBlank` |
| `remittanceInformation` | Yes | `@NotBlank` |
| `debtorAddressLines` | No | Optional list |
| `creditorAddressLines` | No | Optional list |
| `instrForNxtAgt` | No | Optional |

**Check responses:**
- `200` — `data` is `SendResponseDto` with MIR and reference
- `400` — `data` field is null or validation failed

---

### POST `/gw/send-ack-nak`

**Request body — `data` is `SendAckNakRequest`:**
```json
{
  "requestId": "req-004",
  "requestDt": "2026-03-14T10:00:00",
  "data": {
    "type": "ACK",
    "datetime": "2603041641",
    "mir": "260304LBBCLALABXXX0001000007"
  }
}
```

| Field | Validation | Values |
|---|---|---|
| `type` | `@NotBlank`, `@Pattern` | `ACK` or `NAK` only |
| `datetime` | `@NotBlank` | From received message |
| `mir` | `@NotBlank` | From received message |

**Check responses:**
- `200` — ACK/NAK sent, `data` is null
- `400` — `data` null or `type` not `ACK`/`NAK`

---

### POST `/gw/financial-transaction`

Same request structure as `/gw/send`. This is the **full pipeline** variant — it builds the SWIFT message internally before sending.

> Verify description clearly differentiates this from `/gw/send`.

---

## 6. Documentation Quality Checklist

For each endpoint, verify:

- [ ] `@Tag` name is `"Gateway Integration"` with a clear description
- [ ] `@Operation` has a concise `summary` (< 120 chars)
- [ ] `@Operation` has a meaningful `description` explaining the full flow
- [ ] All `@ApiResponse` codes are documented (`200`, `400` at minimum)
- [ ] Each `@ApiResponse` has a `description` explaining when it applies
- [ ] `@ApiResponse` content schema points to the correct model (`ApiResponse.class`)
- [ ] Request DTO fields have `@Schema(description, example)`
- [ ] Required fields are annotated with JSR-303 constraints (`@NotBlank`, `@NotNull`, `@Positive`)
- [ ] Optional fields are clearly marked (no required constraint)
- [ ] Hidden fields use `@Schema(hidden = true)` (e.g., `sessionId` in `FinancialTransactionRequest`)
- [ ] Password fields do NOT expose real values in examples
- [ ] `resStatus` behavior is described in the `200` response description

---

## 7. Error Response Structure

When `resStatus` is `FAILED`, the `error` object is populated:

```json
{
  "resStatus": "FAILED",
  "resCode": "VALIDATION-001",
  "resMessage": "Request data cannot be null",
  "error": {
    "errorCode": "VALIDATION-001",
    "errorMessage": "Request data cannot be null",
    "errorCategory": "VALIDATION",
    "fieldErrors": [
      { "field": "data", "message": "must not be null" }
    ]
  }
}
```

| `errorCategory` | When used |
|---|---|
| `VALIDATION` | Bean validation failures, null data |
| `BUSINESS` | Gateway rejected the operation |
| `TECHNICAL` | Connectivity or SOAP fault |
| `SECURITY` | Auth/session failures |

> **Check:** Verify `ErrorDetails` and `FieldError` schemas are visible in the Swagger "Schemas" section.

---

## 8. Things to Verify in Swagger UI

1. **Schemas section** — confirm these models appear:
   - `ApiRequest`
   - `ApiResponse`
   - `LogonRequest`, `LogonResponseDto`
   - `LogoutRequest`
   - `GetUpdatesResponseDto`
   - `FinancialTransactionRequest`, `FinancialTransactionRequest.TransactionData`
   - `SendAckNakRequest`, `SendResponseDto`
   - `ErrorDetails`, `FieldError`

2. **Try-It-Out** — test each endpoint with the provided examples and confirm:
   - `400` is returned when `requestId` or `requestDt` is missing
   - `400` is returned when `data` is null (for endpoints that require it)
   - `resStatus: "OK"` is returned on success

3. **Server URL** — confirm `http://localhost:7003/stp-client` is listed as the server.

4. **Tag grouping** — all 6 endpoints appear under a single `Gateway Integration` tag.

---

## 9. Adding Documentation to New Endpoints

When adding a new endpoint, follow this pattern:

```java
@Operation(
    summary = "Short action description",          // < 120 chars
    description = "Full explanation of the flow, including session handling and what the response contains."
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success or failure — check resStatus",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
    @ApiResponse(responseCode = "400", description = "Specific reason for 400")
})
@PostMapping("/new-endpoint")
public ResponseEntity<ApiResponse<YourResponseDto>> newEndpoint(
        @Valid @RequestBody ApiRequest<YourRequestDto> request) { ... }
```

And for each DTO:

```java
@Schema(description = "Describe the DTO purpose")
public record YourRequestDto(
    @NotBlank
    @Schema(description = "Field purpose", example = "realistic-example-value")
    String fieldName
) {}
```
