package com.noh.stpclient.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.noh.stpclient.config.interceptor.SoapPayloadLoggingInterceptor;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import com.noh.stpclient.web.dto.SendRequest;
import com.noh.stpclient.web.dto.SendResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Assembles and persists an AuditLog entry after each gateway operation.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #record} reads the SOAP XML ThreadLocal and clears it synchronously
 *       on the calling thread, then fires an async save via the Spring proxy.
 *   <li>{@link #persistAsync} runs on Spring's task executor, builds the AuditLog,
 *       and delegates to AuditRepository — never blocking the original request.
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final AuditRepository auditRepository;

    // Self-injection via proxy: calling persistAsync() on 'this' bypasses Spring AOP,
    // so @Async would not apply. Using the proxy reference ensures the async behaviour works.
    @Lazy
    @Autowired
    private AuditService self;

    /**
     * Called synchronously by GwIntegrationService immediately after each audited operation.
     * Reads and clears the SOAP ThreadLocal on the calling thread, then dispatches an async save.
     */
    public void record(AuditLog.Operation operation,
                       String sessionId,
                       Object request,
                       ServiceResult<?> result) {
        // Must read ThreadLocal HERE on the calling thread — @Async runs on a different thread
        String soapReq = SoapPayloadLoggingInterceptor.getSoapRequestXml();
        String soapRes = SoapPayloadLoggingInterceptor.getSoapResponseXml();
        SoapPayloadLoggingInterceptor.clearSoapPayloads();

        String jsonRequest  = toJson(request);
        String jsonResponse = toJson(result);

        // Extract transaction detail for SEND operations
        AuditLog.TransactionDetail txDetail = null;
        if (request instanceof SendRequest sr) {
            SendResponseDto resp = result.getData() instanceof SendResponseDto dto ? dto : null;
            boolean isSuccessCode = resp != null && "0000".equals(resp.code());
            txDetail = AuditLog.TransactionDetail.builder()
                    .senderBic(sr.message().msgSender())
                    .receiverBic(sr.message().msgReceiver())
                    .msgSequence(sr.message().msgSequence())
                    .resCode(resp != null ? resp.code() : null)
                    .resMessage(resp != null ? resp.description() : null)
                    .resStatus(resp != null ? resp.info() : null)
                    .responseType(isSuccessCode ? resp.type() : null)
                    .responseDatetime(isSuccessCode ? resp.datetime() : null)
                    .responseMir(isSuccessCode ? resp.mir() : null)
                    .responseRef(isSuccessCode ? resp.ref() : null)
                    .build();
        } else if (request instanceof FinancialTransactionRequest ftr && ftr.transaction() != null) {
            var tx = ftr.transaction();
            txDetail = AuditLog.TransactionDetail.builder()
                    .msgId(tx.messageId())
                    .msgSequence(tx.msgSequence())
                    .businessMsgId(tx.businessMessageId())
                    .senderBic(tx.senderBic())
                    .receiverBic(tx.receiverBic())
                    .instructingAgentBic(tx.instructingAgentBic())
                    .instructedAgentBic(tx.instructedAgentBic())
                    .debtorAgentBic(tx.debtorAgentBic())
                    .currency(tx.currency())
                    .amount(tx.amount())
                    .settlementDate(tx.settlementDate())
                    .debtorName(tx.debtorName())
                    .debtorAccount(tx.debtorAccount())
                    .debtorAgentAccount(tx.debtorAgentAccount())
                    .creditorName(tx.creditorName())
                    .creditorAccount(tx.creditorAccount())
                    .creditorAgentAccount(tx.creditorAgentAccount())
                    .debtorAddressLines(tx.debtorAddressLines() != null
                            ? String.join("|", tx.debtorAddressLines()) : null)
                    .instrForNxtAgt(tx.instrForNxtAgt())
                    .remittanceInfo(tx.remittanceInformation())
                    .build();
        }

        // Fire-and-forget: call through proxy so @Async is applied — no ThreadLocal dependency in async thread
        self.persistAsync(operation, sessionId, jsonRequest, jsonResponse,
                soapReq, soapRes,
                result.isSuccess(), result.getErrorCode(), result.getErrorMessage(),
                txDetail);
    }

    /**
     * Runs on Spring's async executor. Builds and saves the AuditLog.
     * All exceptions are caught and logged; audit failures never affect the caller.
     */
    @Async
    public void persistAsync(AuditLog.Operation operation,
                             String sessionId,
                             String jsonRequest,
                             String jsonResponse,
                             String soapRequest,
                             String soapResponse,
                             boolean success,
                             String errorCode,
                             String errorMessage,
                             AuditLog.TransactionDetail txDetail) {
        try {
            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                    .operation(operation)
                    .sessionId(sessionId)
                    .jsonRequest(jsonRequest)
                    .jsonResponse(jsonResponse)
                    .soapRequest(soapRequest)
                    .soapResponse(soapResponse)
                    .success(success)
                    .errorCode(success ? null : errorCode)
                    .errorMessage(success ? null : errorMessage);

            if (txDetail != null) {
                builder.txnDetail(txDetail);
            }

            auditRepository.save(builder.build());
        } catch (Exception e) {
            log.error("Async audit persist failed for operation [{}]: {}", operation, e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Two-step audit for performSendFinancialTransaction:
    //   1. recordSendBefore  — synchronous INSERT before SOAP call
    //   2. recordSendAfter   — reads ThreadLocal then dispatches async UPDATE
    // -------------------------------------------------------------------------

    /**
     * Inserts a PENDING audit row + request fields into STP_SEND_TXN_DETAIL before
     * the SOAP call is made. Returns the generated audit log ID to pass to
     * {@link #recordSendAfter}.
     */
    public long recordSendBefore(String sessionId, FinancialTransactionRequest request) {
        try {
            String jsonRequest = toJson(request);
            AuditLog.TransactionDetail txDetail = buildTxDetailFromRequest(request);
            return auditRepository.insertPendingSend(sessionId, jsonRequest, txDetail);
        } catch (Exception e) {
            log.error("Failed to insert pending send audit for session [{}]: {}", sessionId, e.getMessage(), e);
            return -1L;
        }
    }

    /**
     * Reads SOAP payloads from the ThreadLocal on the calling thread, then fires an
     * async UPDATE to fill in the result fields.
     */
    public void recordSendAfter(long auditLogId, ServiceResult<?> result) {
        if (auditLogId < 0) return;
        // Must read ThreadLocal HERE on the calling thread — @Async runs on a different thread
        String soapReq = SoapPayloadLoggingInterceptor.getSoapRequestXml();
        String soapRes = SoapPayloadLoggingInterceptor.getSoapResponseXml();
        SoapPayloadLoggingInterceptor.clearSoapPayloads();

        String jsonResponse = toJson(result);
        AuditLog.TransactionDetail responseFields = buildResponseFields(result);

        // Call through proxy so @Async is applied
        self.persistSendResultAsync(auditLogId, result.isSuccess(),
                result.getErrorCode(), result.getErrorMessage(),
                jsonResponse, soapReq, soapRes, responseFields);
    }

    @Async
    public void persistSendResultAsync(long auditLogId, boolean success,
                                       String errorCode, String errorMessage,
                                       String jsonResponse, String soapRequest, String soapResponse,
                                       AuditLog.TransactionDetail responseFields) {
        try {
            auditRepository.updateSendResult(auditLogId, success, errorCode, errorMessage,
                    jsonResponse, soapRequest, soapResponse, responseFields);
        } catch (Exception e) {
            log.error("Failed to update send audit result for auditLogId [{}]: {}", auditLogId, e.getMessage(), e);
        }
    }

    private AuditLog.TransactionDetail buildTxDetailFromRequest(FinancialTransactionRequest request) {
        if (request == null || request.transaction() == null) return null;
        var tx = request.transaction();
        return AuditLog.TransactionDetail.builder()
                .msgId(tx.messageId())
                .msgSequence(tx.msgSequence())
                .businessMsgId(tx.businessMessageId())
                .senderBic(tx.senderBic())
                .receiverBic(tx.receiverBic())
                .instructingAgentBic(tx.instructingAgentBic())
                .instructedAgentBic(tx.instructedAgentBic())
                .debtorAgentBic(tx.debtorAgentBic())
                .currency(tx.currency())
                .amount(tx.amount())
                .settlementDate(tx.settlementDate())
                .debtorName(tx.debtorName())
                .debtorAccount(tx.debtorAccount())
                .debtorAgentAccount(tx.debtorAgentAccount())
                .creditorName(tx.creditorName())
                .creditorAccount(tx.creditorAccount())
                .creditorAgentAccount(tx.creditorAgentAccount())
                .debtorAddressLines(tx.debtorAddressLines() != null
                        ? String.join("|", tx.debtorAddressLines()) : null)
                .instrForNxtAgt(tx.instrForNxtAgt())
                .remittanceInfo(tx.remittanceInformation())
                .build();
    }

    private AuditLog.TransactionDetail buildResponseFields(ServiceResult<?> result) {
        if (result.getData() instanceof SendResponseDto resp) {
            if (resp.type() != null) {
                // ACK — save full detail: type, datetime, mir, ref
                return AuditLog.TransactionDetail.builder()
                        .resCode(resp.code())
                        .resMessage(resp.description())
                        .resStatus(resp.info())
                        .responseType(resp.type())
                        .responseDatetime(resp.datetime())
                        .responseMir(resp.mir())
                        .responseRef(resp.ref())
                        .build();
            }
            // NAK — save code, description, and info from SOAP response
            return AuditLog.TransactionDetail.builder()
                    .resCode(resp.code())
                    .resMessage(resp.description())
                    .resStatus(resp.info())
                    .build();
        }
        // Exception / connectivity failure — store error code/message so the row is still updated
        return AuditLog.TransactionDetail.builder()
                .resCode(result.getErrorCode())
                .resMessage(result.getErrorMessage())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise object to JSON for audit: {}", e.getMessage());
            return "{\"_error\":\"serialisation_failed\"}";
        }
    }
}
