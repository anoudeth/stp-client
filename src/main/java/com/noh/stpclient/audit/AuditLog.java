package com.noh.stpclient.audit;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable audit log entry for a gateway SOAP operation.
 * Core fields map to STP_AUDIT_LOG; txnDetail maps to STP_SEND_TXN_DETAIL.
 */
@Getter
@Builder
public class AuditLog {

    public enum Operation {
        SEND,
        GET_UPDATES,
        SEND_ACK_NAK
    }

    private final Operation     operation;
    private final String        sessionId;
    private final String        jsonRequest;
    private final String        jsonResponse;
    private final String        soapRequest;
    private final String        soapResponse;
    private final boolean       success;
    private final String        errorCode;
    private final String        errorMessage;

    @Builder.Default
    private final LocalDateTime createdAt = LocalDateTime.now();

    /** Populated for SEND operations only; null otherwise. Persisted to STP_SEND_TXN_DETAIL. */
    private final TransactionDetail txnDetail;

    @Getter
    @Builder
    public static class TransactionDetail {
        private final String     msgId;
        private final String     msgSequence;
        private final String     businessMsgId;
        private final String     senderBic;
        private final String     receiverBic;
        private final String     instructingAgentBic;
        private final String     instructedAgentBic;
        private final String     debtorAgentBic;
        private final String     currency;
        private final BigDecimal amount;
        private final String     settlementDate;
        private final String     debtorName;
        private final String     debtorAccount;
        private final String     debtorAgentAccount;
        private final String     creditorName;
        private final String     creditorAccount;
        private final String     creditorAgentAccount;
        /** List<String> joined with '|' */
        private final String     debtorAddressLines;
        private final String     instrForNxtAgt;
        private final String     remittanceInfo;
    }
}
