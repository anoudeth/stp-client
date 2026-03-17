---
name: pacs009-builder
description: Build or troubleshoot pacs.009 DataPDU XML for RTGS financial transactions. Use when creating a new FinancialTransactionRequest, debugging XML structure, or validating AppHdr/Document fields.
allowed tools: Read, Grep
---

# pacs.009 DataPDU Builder — STP Client

## Message Structure
```
DataPDU
├── AppHdr (BAH — Business Application Header)
│   ├── Fr  → From BIC (sender)
│   ├── To  → To BIC (RTGS receiver: LPDRLALAXATS)
│   ├── BizMsgIdr  → Business Message ID
│   ├── MsgDefIdr  → "pacs.009.001.08"
│   ├── CreDt      → Creation datetime (ISO8601)
│   └── Sgntr      → XAdES signature (appended by CryptoManager)
└── Document (pacs.009.001.08)
    └── FIToFICstmrCdtTrf
        ├── GrpHdr
        │   ├── MsgId       → unique message ID
        │   ├── CreDtTm     → creation datetime
        │   ├── NbOfTxs     → "1"
        │   └── SttlmInf    → SettlementMethod: INDA
        └── CdtTrfTxInf (Credit Transfer Info)
            ├── PmtId       → InstrId + EndToEndId + UETR
            ├── PmtTpInf    → RTGS ClrChanl, SvcLvl, LclInstrm
            ├── IntrBkSttlmAmt  → amount + currency
            ├── IntrBkSttlmDt   → settlement date
            ├── InstgAgt    → Instructing Agent BIC
            ├── InstdAgt    → Instructed Agent BIC
            ├── Dbtr        → Debtor (name + address)
            ├── DbtrAcct    → Debtor account
            ├── DbtrAgt     → Debtor Agent BIC
            ├── DbtrAgtAcct → Debtor Agent account
            ├── CdtrAgt     → Creditor Agent BIC
            ├── CdtrAgtAcct → Creditor Agent account
            ├── Cdtr        → Creditor (name)
            ├── CdtrAcct    → Creditor account
            └── RmtInf      → Remittance info (Ustrd)
```

## FinancialTransactionRequest Fields
Located: `web/dto/FinancialTransactionRequest.java`
```java
record TransactionData(
    String msgId,           // Unique message ID
    String msgSequence,     // Sequence number
    String senderBic,       // Sender BIC (e.g. LBBCLALABXXX)
    String receiverBic,     // RTGS system BIC
    String businessMsgId,   // BAH business message identifier
    String instrId,         // Instruction ID
    String endToEndId,      // End-to-end ID
    String uetr,            // UUID format (Unique End-to-end Transaction Reference)
    String currency,        // ISO 4217 (e.g. LAK, USD)
    String amount,          // Decimal string (e.g. "1500000.00")
    String settlementDate,  // ISO date (e.g. "2026-03-17")
    String instructingAgentBic,
    String instructedAgentBic,
    String debtorName,
    String debtorAddressLine1,
    String debtorAddressLine2,
    String debtorAccount,
    String debtorAgentBic,
    String debtorAgentAccount,
    String creditorAgentBic,
    String creditorAgentAccount,
    String creditorName,
    String creditorAccount,
    String instrForNxtAgt,  // Instruction for next agent (optional)
    String remittanceInfo   // Free text remittance information
)
```

## Transformation Entry Point
`DataPDUTransformer.transformToDataPDU(FinancialTransactionRequest)`
→ `marshalToXml(DataPDU)` → JAXB string
→ `CryptoManager.signXml(xmlString)` → XAdES signed XML
→ Set as `block4` in SOAP Send request (wrapped in CDATA)

## Validation Points
- `uetr` must be UUID format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- `amount` must be parseable as BigDecimal
- `settlementDate` must be ISO date `YYYY-MM-DD`
- `senderBic` must be 11-char BIC: `LBBCLALABXXX`
- `currency` must be ISO 4217

## RTGS Constants (GwIntegrationService)
```java
RTGS_MSG_TYPE = "pacs.009.001.08"
RTGS_FORMAT   = "MX"
rtgsMsgReceiver = "${stp.soap.rtgs-receiver}"  // LPDRLALAXATS
```
