-- ============================================================
-- STP Session Table
-- ============================================================

CREATE TABLE STP_SESSION (
    USERNAME    VARCHAR2(100) NOT NULL,
    SESSION_ID  VARCHAR2(100) NOT NULL,
    CREATED_AT  TIMESTAMP     NOT NULL,
    CONSTRAINT  STP_SESSION_PK PRIMARY KEY (USERNAME)
);

-- ============================================================
-- STP Audit Tables
-- ============================================================

CREATE TABLE STP_AUDIT_LOG (
    ID              NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    OPERATION       VARCHAR2(30)    NOT NULL,
    SESSION_ID      VARCHAR2(100),
    JSON_REQUEST    CLOB,
    JSON_RESPONSE   CLOB,
    SOAP_REQUEST    CLOB,
    SOAP_RESPONSE   CLOB,
    STATUS          VARCHAR2(10)    NOT NULL,
    ERROR_CODE      VARCHAR2(50),
    ERROR_MESSAGE   VARCHAR2(500),
    CREATED_AT      TIMESTAMP       NOT NULL
);

CREATE TABLE STP_SEND_TXN_DETAIL (
    ID                      NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    AUDIT_LOG_ID            NUMBER          NOT NULL,
    MSG_ID                  VARCHAR2(100),
    MSG_SEQUENCE            VARCHAR2(50),
    BUSINESS_MSG_ID         VARCHAR2(100),
    SENDER_BIC              VARCHAR2(20),
    RECEIVER_BIC            VARCHAR2(20),
    INSTRUCTING_AGENT_BIC   VARCHAR2(20),
    INSTRUCTED_AGENT_BIC    VARCHAR2(20),
    DEBTOR_AGENT_BIC        VARCHAR2(20),
    CURRENCY                VARCHAR2(3),
    AMOUNT                  NUMBER(20, 5),
    SETTLEMENT_DATE         VARCHAR2(20),
    DEBTOR_NAME             VARCHAR2(200),
    DEBTOR_ACCOUNT          VARCHAR2(100),
    DEBTOR_AGENT_ACCOUNT    VARCHAR2(100),
    CREDITOR_NAME           VARCHAR2(200),
    CREDITOR_ACCOUNT        VARCHAR2(100),
    CREDITOR_AGENT_ACCOUNT  VARCHAR2(100),
    DEBTOR_ADDRESS_LINES    VARCHAR2(500),
    INSTR_FOR_NXT_AGT       VARCHAR2(500),
    REMITTANCE_INFO         VARCHAR2(500),
    STATUS                  VARCHAR2(10)    NOT NULL,
    RES_CODE                VARCHAR2(10),
    RES_MESSAGE             VARCHAR2(500),
    RES_STATUS              VARCHAR2(100),
    RESPONSE_TYPE           VARCHAR2(10),
    RESPONSE_DATETIME       VARCHAR2(50),
    RESPONSE_MIR            VARCHAR2(100),
    RESPONSE_REF            VARCHAR2(100),
    CONSTRAINT FK_TXN_AUDIT FOREIGN KEY (AUDIT_LOG_ID) REFERENCES STP_AUDIT_LOG(ID)
);
