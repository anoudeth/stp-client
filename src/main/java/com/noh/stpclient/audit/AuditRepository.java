package com.noh.stpclient.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILURE = "FAILURE";

    private static final String INSERT_AUDIT = """
            INSERT INTO STP_AUDIT_LOG
                (OPERATION, SESSION_ID, JSON_REQUEST, JSON_RESPONSE,
                 SOAP_REQUEST, SOAP_RESPONSE, STATUS, ERROR_CODE, ERROR_MESSAGE, CREATED_AT)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_TXN = """
            INSERT INTO STP_SEND_TXN_DETAIL
                (AUDIT_LOG_ID, STATUS, MSG_ID, MSG_SEQUENCE, BUSINESS_MSG_ID,
                 SENDER_BIC, RECEIVER_BIC, INSTRUCTING_AGENT_BIC, INSTRUCTED_AGENT_BIC, DEBTOR_AGENT_BIC,
                 CURRENCY, AMOUNT, SETTLEMENT_DATE,
                 DEBTOR_NAME, DEBTOR_ACCOUNT, DEBTOR_AGENT_ACCOUNT,
                 CREDITOR_NAME, CREDITOR_ACCOUNT, CREDITOR_AGENT_ACCOUNT,
                 DEBTOR_ADDRESS_LINES, INSTR_FOR_NXT_AGT, REMITTANCE_INFO,
                 RES_CODE, RES_MESSAGE, RES_STATUS,
                 RESPONSE_TYPE, RESPONSE_DATETIME, RESPONSE_MIR, RESPONSE_REF)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

    private static final String INSERT_PENDING_SEND_AUDIT = """
            INSERT INTO STP_AUDIT_LOG
                (OPERATION, SESSION_ID, JSON_REQUEST, STATUS, CREATED_AT)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String INSERT_TXN_REQUEST_ONLY = """
            INSERT INTO STP_SEND_TXN_DETAIL
                (AUDIT_LOG_ID, STATUS, MSG_ID, MSG_SEQUENCE, BUSINESS_MSG_ID,
                 SENDER_BIC, RECEIVER_BIC, INSTRUCTING_AGENT_BIC, INSTRUCTED_AGENT_BIC, DEBTOR_AGENT_BIC,
                 CURRENCY, AMOUNT, SETTLEMENT_DATE,
                 DEBTOR_NAME, DEBTOR_ACCOUNT, DEBTOR_AGENT_ACCOUNT,
                 CREDITOR_NAME, CREDITOR_ACCOUNT, CREDITOR_AGENT_ACCOUNT,
                 DEBTOR_ADDRESS_LINES, INSTR_FOR_NXT_AGT, REMITTANCE_INFO)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

    private static final String UPDATE_SEND_AUDIT_RESULT = """
            UPDATE STP_AUDIT_LOG
            SET STATUS = ?, JSON_RESPONSE = ?, SOAP_REQUEST = ?, SOAP_RESPONSE = ?,
                ERROR_CODE = ?, ERROR_MESSAGE = ?
            WHERE ID = ?
            """;

    private static final String UPDATE_TXN_RESPONSE = """
            UPDATE STP_SEND_TXN_DETAIL
            SET STATUS = ?, RES_CODE = ?, RES_MESSAGE = ?, RES_STATUS = ?,
                RESPONSE_TYPE = ?, RESPONSE_DATETIME = ?, RESPONSE_MIR = ?, RESPONSE_REF = ?
            WHERE AUDIT_LOG_ID = ?
            """;

    private static final String UPDATE_AUDIT_RESULT = """
            UPDATE STP_AUDIT_LOG
            SET STATUS = ?, JSON_RESPONSE = ?, SOAP_REQUEST = ?, SOAP_RESPONSE = ?,
                ERROR_CODE = ?, ERROR_MESSAGE = ?
            WHERE ID = ?
            """;

    private static final String INSERT_GET_UPDATE_ITEM = """
            INSERT INTO STP_GET_UPDATE
                (MSG_TYPE, MSG_ID, MSG_SENDER, MSG_RECEIVER, MSG_FORMAT, MSG_SUB_FORMAT, FORMAT,
                 MSG_SESSION, MSG_SEQUENCE, MSG_PRIORITY, MSG_USER_PRIORITY, MSG_USER_REFERENCE,
                 MSG_NET_MIR, MSG_NET_INPUT_TIME, MSG_NET_OUTPUT_DATE, MSG_PAC_RESULT,
                 MSG_FIN_VALIDATION, MSG_PDE, MSG_PDM, MSG_COPY_SRV_ID, MSG_COPY_SRV_INFO,
                 MSG_DEL_NOTIF_RQ, BLOCK4, CREATED_AT)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Inserts a PENDING audit row + request-only txn detail row before the SOAP call.
     * Both inserts are wrapped in a transaction — if the txn detail insert fails,
     * the audit log row is rolled back.
     * Returns the generated STP_AUDIT_LOG ID for use in {@link #updateSendResult}.
     */
    @Transactional
    public long insertPendingSend(String sessionId, String jsonRequest,
                                  AuditLog.TransactionDetail txDetail) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            OracleConnection oc = connection.unwrap(OracleConnection.class);
            PreparedStatement ps = connection.prepareStatement(INSERT_PENDING_SEND_AUDIT, new String[]{"ID"});
            ps.setString   (1, AuditLog.Operation.SEND.name());
            ps.setString   (2, sessionId);
            ps.setClob     (3, toClob(oc, jsonRequest));
            ps.setString   (4, STATUS_PENDING);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        long auditLogId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        if (txDetail != null) {
            jdbcTemplate.update(INSERT_TXN_REQUEST_ONLY,
                    auditLogId,
                    STATUS_PENDING,
                    txDetail.getMsgId(),
                    txDetail.getMsgSequence(),
                    txDetail.getBusinessMsgId(),
                    txDetail.getSenderBic(),
                    txDetail.getReceiverBic(),
                    txDetail.getInstructingAgentBic(),
                    txDetail.getInstructedAgentBic(),
                    txDetail.getDebtorAgentBic(),
                    txDetail.getCurrency(),
                    txDetail.getAmount(),
                    txDetail.getSettlementDate(),
                    truncate(txDetail.getDebtorName(), 200),
                    txDetail.getDebtorAccount(),
                    txDetail.getDebtorAgentAccount(),
                    truncate(txDetail.getCreditorName(), 200),
                    txDetail.getCreditorAccount(),
                    txDetail.getCreditorAgentAccount(),
                    truncate(txDetail.getDebtorAddressLines(), 500),
                    truncate(txDetail.getInstrForNxtAgt(), 500),
                    truncate(txDetail.getRemittanceInfo(), 500)
            );
        }
        return auditLogId;
    }

    /**
     * Updates the PENDING audit row to SUCCESS/FAILURE and populates response fields
     * in STP_SEND_TXN_DETAIL after the SOAP call completes.
     * Both updates are wrapped in a transaction.
     */
    @Transactional
    public void updateSendResult(long auditLogId, boolean success,
                                 String errorCode, String errorMessage,
                                 String jsonResponse, String soapRequest, String soapResponse,
                                 AuditLog.TransactionDetail responseFields) {
        String status = success ? STATUS_SUCCESS : STATUS_FAILURE;

        jdbcTemplate.update(connection -> {
            OracleConnection oc = connection.unwrap(OracleConnection.class);
            PreparedStatement ps = connection.prepareStatement(UPDATE_SEND_AUDIT_RESULT);
            ps.setString(1, status);
            ps.setClob  (2, toClob(oc, jsonResponse));
            ps.setClob  (3, toClob(oc, soapRequest));
            ps.setClob  (4, toClob(oc, soapResponse));
            ps.setString(5, success ? null : errorCode);
            ps.setString(6, success ? null : truncate(errorMessage, 500));
            ps.setLong  (7, auditLogId);
            return ps;
        });

        // Always update txn detail STATUS — even when responseFields is null
        String resCode    = responseFields != null ? responseFields.getResCode()        : null;
        String resMessage = responseFields != null ? truncate(responseFields.getResMessage(), 500) : null;
        String resStatus  = responseFields != null ? responseFields.getResStatus()      : null;
        String respType   = responseFields != null ? responseFields.getResponseType()   : null;
        String respDt     = responseFields != null ? responseFields.getResponseDatetime(): null;
        String respMir    = responseFields != null ? responseFields.getResponseMir()    : null;
        String respRef    = responseFields != null ? responseFields.getResponseRef()    : null;

        jdbcTemplate.update(UPDATE_TXN_RESPONSE,
                status, resCode, resMessage, resStatus,
                respType, respDt, respMir, respRef,
                auditLogId
        );
    }

    /**
     * Inserts a PENDING audit row for GET_UPDATES before the SOAP call.
     * Returns the generated STP_AUDIT_LOG ID for use in {@link #updateGetUpdatesResult}.
     */
    @Transactional
    public long insertPendingGetUpdates(String sessionId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_PENDING_SEND_AUDIT, new String[]{"ID"});
            ps.setString   (1, AuditLog.Operation.GET_UPDATES.name());
            ps.setString   (2, sessionId);
            ps.setString   (3, null);
            ps.setString   (4, STATUS_PENDING);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    /**
     * Updates the PENDING audit row to SUCCESS/FAILURE after the SOAP call completes.
     */
    @Transactional
    public void updateGetUpdatesResult(long auditLogId, boolean success,
                                       String errorCode, String errorMessage,
                                       String jsonResponse, String soapRequest, String soapResponse) {
        String status = success ? STATUS_SUCCESS : STATUS_FAILURE;
        jdbcTemplate.update(connection -> {
            OracleConnection oc = connection.unwrap(OracleConnection.class);
            PreparedStatement ps = connection.prepareStatement(UPDATE_AUDIT_RESULT);
            ps.setString(1, status);
            ps.setClob  (2, toClob(oc, jsonResponse));
            ps.setClob  (3, toClob(oc, soapRequest));
            ps.setClob  (4, toClob(oc, soapResponse));
            ps.setString(5, success ? null : errorCode);
            ps.setString(6, success ? null : truncate(errorMessage, 500));
            ps.setLong  (7, auditLogId);
            return ps;
        });
    }

    public void insertGetUpdateItem(String msgType, String msgId,
                                    String msgSender, String msgReceiver, String msgFormat,
                                    String msgSubFormat, String format, String msgSession,
                                    String msgSequence, String msgPriority, String msgUserPriority,
                                    String msgUserReference, String msgNetMir, String msgNetInputTime,
                                    String msgNetOutputDate, String msgPacResult, String msgFinValidation,
                                    String msgPde, String msgPdm, String msgCopySrvId,
                                    String msgCopySrvInfo, String msgDelNotifRq, String block4) {
        jdbcTemplate.update(connection -> {
            OracleConnection oc = connection.unwrap(OracleConnection.class);
            PreparedStatement ps = connection.prepareStatement(INSERT_GET_UPDATE_ITEM);
            ps.setString   (1,  truncate(msgType, 50));
            ps.setString   (2,  truncate(msgId, 100));
            ps.setString   (3,  truncate(msgSender, 20));
            ps.setString   (4,  truncate(msgReceiver, 20));
            ps.setString   (5,  truncate(msgFormat, 10));
            ps.setString   (6,  truncate(msgSubFormat, 10));
            ps.setString   (7,  truncate(format, 10));
            ps.setString   (8,  truncate(msgSession, 10));
            ps.setString   (9,  truncate(msgSequence, 20));
            ps.setString   (10, truncate(msgPriority, 5));
            ps.setString   (11, truncate(msgUserPriority, 10));
            ps.setString   (12, truncate(msgUserReference, 100));
            ps.setString   (13, truncate(msgNetMir, 100));
            ps.setString   (14, truncate(msgNetInputTime, 10));
            ps.setString   (15, truncate(msgNetOutputDate, 20));
            ps.setString   (16, truncate(msgPacResult, 50));
            ps.setString   (17, truncate(msgFinValidation, 50));
            ps.setString   (18, truncate(msgPde, 5));
            ps.setString   (19, truncate(msgPdm, 5));
            ps.setString   (20, truncate(msgCopySrvId, 50));
            ps.setString   (21, truncate(msgCopySrvInfo, 200));
            ps.setString   (22, truncate(msgDelNotifRq, 5));
            ps.setClob     (23, toClob(oc, block4));
            ps.setTimestamp(24, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        });
    }

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
                ps.setString   (7,  auditLog.isSuccess() ? STATUS_SUCCESS : STATUS_FAILURE);
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
                        auditLog.isSuccess() ? STATUS_SUCCESS : STATUS_FAILURE,
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
        if (value == null) return null;
        java.sql.Clob clob = conn.createClob();
        clob.setString(1, value);
        return clob;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
