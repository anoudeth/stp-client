package com.noh.stpclient.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.noh.stpclient.config.interceptor.SoapPayloadLoggingInterceptor;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Assembles and persists an AuditLog entry after each gateway operation.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #record} reads the SOAP XML ThreadLocal and clears it synchronously
 *       on the calling thread, then fires an async save.
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
        if (request instanceof FinancialTransactionRequest ftr && ftr.transaction() != null) {
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

        // Fire-and-forget: all values passed as params — no ThreadLocal dependency in async thread
        persistAsync(operation, sessionId, jsonRequest, jsonResponse,
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
