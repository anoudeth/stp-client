package com.noh.stpclient.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Executes audit persistence operations on Spring's async executor.
 *
 * <p>Extracted from {@link AuditService} so that {@code @Async} is applied via a
 * normal bean dependency rather than the fragile {@code @Lazy} self-injection pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncAuditPersister {

    private final AuditRepository auditRepository;

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
        log.info("[AUDIT] persistAsync start — operation={}, session={}, success={}", operation, sessionId, success);
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
            log.info("[AUDIT] persistAsync complete — operation={}, session={}", operation, sessionId);
        } catch (Exception e) {
            log.error("[AUDIT] persistAsync failed — operation={}, session={}: {}", operation, sessionId, e.getMessage(), e);
        }
    }

    @Async
    public void persistSendResultAsync(long auditLogId, boolean success,
                                       String errorCode, String errorMessage,
                                       String jsonResponse, String soapRequest, String soapResponse,
                                       AuditLog.TransactionDetail responseFields) {
        log.info("[AUDIT] persistSendResultAsync start — auditLogId={}, success={}", auditLogId, success);
        try {
            auditRepository.updateSendResult(auditLogId, success, errorCode, errorMessage,
                    jsonResponse, soapRequest, soapResponse, responseFields);
            log.info("[AUDIT] persistSendResultAsync complete — auditLogId={}, status={}",
                    auditLogId, success ? "SUCCESS" : "FAILURE");
        } catch (Exception e) {
            log.error("[AUDIT] persistSendResultAsync failed — auditLogId={}: {}", auditLogId, e.getMessage(), e);
        }
    }

    @Async
    public void persistGetUpdatesResultAsync(long auditLogId, boolean success,
                                             String errorCode, String errorMessage,
                                             String jsonResponse, String soapRequest, String soapResponse) {
        log.info("[AUDIT] persistGetUpdatesResultAsync start — auditLogId={}, success={}", auditLogId, success);
        try {
            auditRepository.updateGetUpdatesResult(auditLogId, success, errorCode, errorMessage,
                    jsonResponse, soapRequest, soapResponse);
            log.info("[AUDIT] persistGetUpdatesResultAsync complete — auditLogId={}, status={}",
                    auditLogId, success ? "SUCCESS" : "FAILURE");
        } catch (Exception e) {
            log.error("[AUDIT] persistGetUpdatesResultAsync failed — auditLogId={}: {}", auditLogId, e.getMessage(), e);
        }
    }
}