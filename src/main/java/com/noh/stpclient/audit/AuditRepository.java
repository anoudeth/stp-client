package com.noh.stpclient.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Persists AuditLog entries to Oracle.
 * Core audit data goes to STP_AUDIT_LOG; SEND transaction details go to STP_SEND_TXN_DETAIL.
 * Uses OracleConnection.createClob() to handle CLOB columns — Oracle's setString silently
 * truncates at 4000 chars.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AuditRepository {

    private static final String INSERT_AUDIT = """
            INSERT INTO STP_AUDIT_LOG
                (OPERATION, SESSION_ID, JSON_REQUEST, JSON_RESPONSE,
                 SOAP_REQUEST, SOAP_RESPONSE, STATUS, ERROR_CODE, ERROR_MESSAGE, CREATED_AT)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_TXN = """
            INSERT INTO STP_SEND_TXN_DETAIL
                (AUDIT_LOG_ID, MSG_ID, MSG_SEQUENCE, BUSINESS_MSG_ID,
                 SENDER_BIC, RECEIVER_BIC, INSTRUCTING_AGENT_BIC, INSTRUCTED_AGENT_BIC, DEBTOR_AGENT_BIC,
                 CURRENCY, AMOUNT, SETTLEMENT_DATE,
                 DEBTOR_NAME, DEBTOR_ACCOUNT, DEBTOR_AGENT_ACCOUNT,
                 CREDITOR_NAME, CREDITOR_ACCOUNT, CREDITOR_AGENT_ACCOUNT,
                 DEBTOR_ADDRESS_LINES, INSTR_FOR_NXT_AGT, REMITTANCE_INFO,
                 RES_CODE, RES_MESSAGE, RES_STATUS,
                 RESPONSE_TYPE, RESPONSE_DATETIME, RESPONSE_MIR, RESPONSE_REF)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void save(AuditLog auditLog) {
        try {
            // 1. Insert core audit row; retrieve generated ID
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                OracleConnection oc = connection.unwrap(OracleConnection.class);
                PreparedStatement ps = connection.prepareStatement(INSERT_AUDIT, new String[]{"ID"});
                ps.setString   (1,  auditLog.getOperation().name());
                ps.setString   (2,  auditLog.getSessionId());
                ps.setClob     (3,  toClob(oc, auditLog.getJsonRequest()));
                ps.setClob     (4,  toClob(oc, auditLog.getJsonResponse()));
                ps.setClob     (5,  toClob(oc, auditLog.getSoapRequest()));
                ps.setClob     (6,  toClob(oc, auditLog.getSoapResponse()));
                ps.setString   (7,  auditLog.isSuccess() ? "SUCCESS" : "FAILURE");
                ps.setString   (8,  auditLog.getErrorCode());
                ps.setString   (9,  truncate(auditLog.getErrorMessage(), 500));
                ps.setTimestamp(10, Timestamp.valueOf(auditLog.getCreatedAt()));
                return ps;
            }, keyHolder);

            // 2. Insert transaction detail row if present (SEND operations)
            if (auditLog.getTxnDetail() != null) {
                long auditLogId = Objects.requireNonNull(keyHolder.getKey()).longValue();
                AuditLog.TransactionDetail tx = auditLog.getTxnDetail();
                jdbcTemplate.update(INSERT_TXN,
                        auditLogId,
                        tx.getMsgId(),
                        tx.getMsgSequence(),
                        tx.getBusinessMsgId(),
                        tx.getSenderBic(),
                        tx.getReceiverBic(),
                        tx.getInstructingAgentBic(),
                        tx.getInstructedAgentBic(),
                        tx.getDebtorAgentBic(),
                        tx.getCurrency(),
                        tx.getAmount(),
                        tx.getSettlementDate(),
                        truncate(tx.getDebtorName(), 200),
                        tx.getDebtorAccount(),
                        tx.getDebtorAgentAccount(),
                        truncate(tx.getCreditorName(), 200),
                        tx.getCreditorAccount(),
                        tx.getCreditorAgentAccount(),
                        truncate(tx.getDebtorAddressLines(), 500),
                        truncate(tx.getInstrForNxtAgt(), 500),
                        truncate(tx.getRemittanceInfo(), 500),
                        tx.getResCode(),
                        truncate(tx.getResMessage(), 500),
                        tx.getResStatus(),
                        tx.getResponseType(),
                        tx.getResponseDatetime(),
                        tx.getResponseMir(),
                        tx.getResponseRef()
                );
            }
        } catch (Exception e) {
            // Audit failure must NEVER propagate to the caller
            log.error("Failed to persist audit log for operation [{}]: {}",
                    auditLog.getOperation(), e.getMessage(), e);
        }
    }

    private java.sql.Clob toClob(OracleConnection conn, String value) throws SQLException {
        java.sql.Clob clob = conn.createClob();
        clob.setString(1, value != null ? value : "");
        return clob;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
