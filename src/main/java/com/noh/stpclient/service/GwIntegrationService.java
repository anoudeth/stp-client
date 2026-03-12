package com.noh.stpclient.service;

import com.noh.stpclient.audit.AuditLog;
import com.noh.stpclient.audit.AuditService;
import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.model.xml.DataPDU;
import com.noh.stpclient.model.xml.GetUpdatesResponse;
import com.noh.stpclient.model.xml.Send;
import com.noh.stpclient.model.xml.SendResponse;
import com.noh.stpclient.model.xml.SendResponseData;
import com.noh.stpclient.remote.GWClientMuRemote;
import com.noh.stpclient.utils.CryptoManager;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import com.noh.stpclient.web.dto.GetUpdatesResponseDto;
import com.noh.stpclient.web.dto.LogonResponseDto;
import com.noh.stpclient.web.dto.SendRequest;
import com.noh.stpclient.web.dto.SendResponseDto;

import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Service layer for Gateway Integration.
 * Orchestrates calls to the SOAP Client and records audit logs for each operation.
 */
@Service
@Slf4j
public class GwIntegrationService {

    private final GWClientMuRemote soapClient;
    private final CryptoManager cryptoManager;
    private final DataPDUTransformer dataPDUTransformer;
    private final AuditService auditService;
    private final SessionManager sessionManager;

    @Value("${stp.soap.rtgs-receiver}")
    private String rtgsMsgReceiver;

    public GwIntegrationService(GWClientMuRemote soapClient,
                                CryptoManager cryptoManager,
                                DataPDUTransformer dataPDUTransformer,
                                AuditService auditService,
                                SessionManager sessionManager,
                                @Value("${stp.soap.rtgs-receiver}") String rtgsMsgReceiver) {
        this.soapClient = soapClient;
        this.cryptoManager = cryptoManager;
        this.dataPDUTransformer = dataPDUTransformer;
        this.auditService = auditService;
        this.sessionManager = sessionManager;
        this.rtgsMsgReceiver = rtgsMsgReceiver;
    }

    public ServiceResult<LogonResponseDto> performLogon() {
        try {
            String sessionId = sessionManager.getOrCreate();
            log.info("Session ID: {}", sessionId);
            return ServiceResult.success(new LogonResponseDto(sessionId));
        } catch (GatewayIntegrationException e) {
            log.error(e.getMessage());
            return ServiceResult.failure(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("Logon failed", e);
            return ServiceResult.failure("GW-999", "Gateway Logon Failed");
        }
    }

    public ServiceResult<Void> performLogout() {
        try {
            sessionManager.logout();
            log.info("Logout successful");
            return ServiceResult.success(null);
        } catch (GatewayIntegrationException e) {
            return ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ServiceResult.failure("GW-999", "Gateway Logout Failed");
        }
    }

    public ServiceResult<List<GetUpdatesResponseDto>> performGetUpdates(String sessionId) {
        Assert.hasText(sessionId, "Session ID must not be empty");

        // 1. Insert PENDING audit row before SOAP call (non-blocking)
        long auditLogId = auditService.recordGetUpdatesBefore(sessionId);

        ServiceResult<List<GetUpdatesResponseDto>> result;
        try {
            result = doGetUpdates(sessionId);
        } catch (GatewayIntegrationException e) {
            if (sessionManager.isSessionExpired(e)) {
                log.warn("Session {} expired during GetUpdates — refreshing and retrying", sessionId);
                sessionManager.invalidate(sessionId);
                String freshSession = sessionManager.getOrCreate();
                try {
                    result = doGetUpdates(freshSession);
                } catch (GatewayIntegrationException retryEx) {
                    result = ServiceResult.failure(retryEx.getCode(), retryEx.getDescription());
                } catch (Exception retryEx) {
                    log.error("GetUpdates retry failed for session: {}", freshSession, retryEx);
                    result = ServiceResult.failure("GW-999", "Gateway GetUpdates Failed");
                }
            } else {
                result = ServiceResult.failure(e.getCode(), e.getDescription());
            }
        } catch (Exception e) {
            log.error("GetUpdates failed for session: {}", sessionId, e);
            result = ServiceResult.failure("GW-999", "Gateway GetUpdates Failed");
        }

        // 2. Async UPDATE with ~3 s delay — does not block the response
        auditService.recordGetUpdatesAfter(auditLogId, result);
        return result;
    }

    private ServiceResult<List<GetUpdatesResponseDto>> doGetUpdates(String sessionId) {
        GetUpdatesResponse response = soapClient.getUpdates(sessionId);
        if (response == null || response.getItems() == null) {
            return ServiceResult.success(Collections.emptyList());
        }
        List<GetUpdatesResponseDto> items = response.getItems().stream()
                .map(GetUpdatesResponseDto::from)
                .toList();
        return ServiceResult.success(items);
    }

    public ServiceResult<SendResponseDto> performSend(SendRequest request) {
        Assert.notNull(request, "Send request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.notNull(request.message(), "Message content cannot be null");

        ServiceResult<SendResponseDto> result;
        try {
            Send soapRequest = new Send();
            soapRequest.setSessionId(request.sessionId());
            Send.Message soapMessage = new Send.Message();
            soapMessage.setBlock4(request.message().block4());
            soapMessage.setMsgReceiver(request.message().msgReceiver());
            soapMessage.setMsgSender(request.message().msgSender());
            soapMessage.setMsgType(request.message().msgType());
            soapMessage.setMsgSequence(request.message().msgSequence());
            soapMessage.setFormat(request.message().format());
            soapRequest.setMessage(soapMessage);

            SendResponse response = soapClient.send(soapRequest);

            if (response == null || response.getData() == null) {
                result = ServiceResult.failure("GW-002", "Received empty response from Gateway");
            } else {
                SendResponseData data = response.getData();
                try {
                    boolean signatureValid = cryptoManager.verifyResponseSignature(data);
                    if (!signatureValid) {
                        log.warn("Gateway response signature verification FAILED for session: {}", request.sessionId());
                    } else {
                        log.info("Gateway response signature verified OK for session: {}", request.sessionId());
                    }
                } catch (Exception e) {
                    log.warn("Gateway response signature verification error for session: {}: {}", request.sessionId(), e.getMessage());
                }
                if ("NAK".equals(data.getType())) {
                    result = ServiceResult.failure(data.getCode(), data.getDescription());
                } else {
                    result = ServiceResult.success(SendResponseDto.from(data));
                }
            }
        } catch (GatewayIntegrationException e) {
            result = ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("Send failed for session: {}", request.sessionId(), e);
            result = ServiceResult.failure("GW-999", "Gateway Send Failed");
        }
        auditService.record(AuditLog.Operation.SEND, request.sessionId(), request, result);
        return result;
    }

    public ServiceResult<Void> performSendAckNak(com.noh.stpclient.web.dto.SendAckNakRequest request) {
        Assert.notNull(request, "SendAckNak request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.hasText(request.type(), "Type must not be empty");
        Assert.hasText(request.datetime(), "Datetime must not be empty");
        Assert.hasText(request.mir(), "MIR must not be empty");

        ServiceResult<Void> result;
        try {
            SendResponseData data = new SendResponseData();
            data.setType(request.type());
            data.setDatetime(request.datetime());
            data.setMir(request.mir());
            soapClient.sendAckNak(request.sessionId(), data);
            result = ServiceResult.success(null);
        } catch (GatewayIntegrationException e) {
            result = ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("SendAckNak failed for mir: {}", request.mir(), e);
            result = ServiceResult.failure("GW-999", "Gateway SendAckNak Failed");
        }
        auditService.record(AuditLog.Operation.SEND_ACK_NAK, request.sessionId(), request, result);
        return result;
    }

    public ServiceResult<SendResponseDto> performSendFinancialTransaction(FinancialTransactionRequest request) {
        Assert.notNull(request, "Financial transaction request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.notNull(request.transaction(), "Transaction data cannot be null");

        // 1. Insert PENDING audit row + request fields before SOAP call
        long auditLogId = auditService.recordSendBefore(request.sessionId(), request);

        ServiceResult<SendResponseDto> result;
        try {
            result = doSendFinancialTransaction(request);
        } catch (GatewayIntegrationException e) {
            if (sessionManager.isSessionExpired(e)) {
                log.warn("Session {} expired during Send — refreshing and retrying", request.sessionId());
                sessionManager.invalidate(request.sessionId());
                String freshSession = sessionManager.getOrCreate();
                FinancialTransactionRequest retryRequest = new FinancialTransactionRequest(freshSession, request.transaction());
                try {
                    result = doSendFinancialTransaction(retryRequest);
                } catch (GatewayIntegrationException retryEx) {
                    result = ServiceResult.failure(retryEx.getCode(), retryEx.getDescription());
                } catch (Exception retryEx) {
                    log.error("Send retry failed for session: {}", freshSession, retryEx);
                    result = ServiceResult.failure("GW-999", "Financial transaction send failed");
                }
            } else {
                result = ServiceResult.failure(e.getCode(), e.getDescription());
            }
        } catch (JAXBException e) {
            log.error("XML marshaling failed for financial transaction", e);
            result = ServiceResult.failure("GW-003", "XML transformation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Financial transaction send failed for session: {}", request.sessionId(), e);
            result = ServiceResult.failure("GW-999", "Financial transaction send failed");
        }

        // 2. Update audit row with result (async — does not block logout)
        auditService.recordSendAfter(auditLogId, result);
        return result;
    }

    private ServiceResult<SendResponseDto> doSendFinancialTransaction(FinancialTransactionRequest request) throws Exception {
        DataPDU dataPDU = dataPDUTransformer.transformToDataPDU(request);
        String xmlContent = dataPDUTransformer.marshalToXml(dataPDU);
        String signedXml = cryptoManager.signXml(xmlContent);

        Send soapRequest = buildSend(request, signedXml);
        log.info("Sending SOAP request block4:\n{}", signedXml);

        SendResponse response = soapClient.send(soapRequest);

        if (response == null || response.getData() == null) {
            return ServiceResult.failure("GW-002", "Received empty response from Gateway");
        }
        SendResponseData data = response.getData();
        if ("NAK".equals(data.getType())) {
            return ServiceResult.failureWithData(SendResponseDto.from(data), data.getCode(), data.getDescription());
        }
        return ServiceResult.success(SendResponseDto.from(data));
    }

    public ServiceResult<SendResponseDto> performFinancialTransaction(FinancialTransactionRequest request) {
        Assert.notNull(request, "Financial transaction request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.notNull(request.transaction(), "Transaction data cannot be null");

        ServiceResult<SendResponseDto> result;
        try {
            result = doFinancialTransaction(request);
        } catch (GatewayIntegrationException e) {
            if (sessionManager.isSessionExpired(e)) {
                log.warn("Session {} expired during financial transaction — refreshing and retrying", request.sessionId());
                sessionManager.invalidate(request.sessionId());
                String freshSession = sessionManager.getOrCreate();
                FinancialTransactionRequest retryRequest = new FinancialTransactionRequest(freshSession, request.transaction());
                try {
                    result = doFinancialTransaction(retryRequest);
                } catch (GatewayIntegrationException retryEx) {
                    result = ServiceResult.failure(retryEx.getCode(), retryEx.getDescription());
                } catch (Exception retryEx) {
                    log.error("Financial transaction retry failed for session: {}", freshSession, retryEx);
                    result = ServiceResult.failure("GW-999", "Financial transaction failed");
                }
            } else {
                result = ServiceResult.failure(e.getCode(), e.getDescription());
            }
        } catch (JAXBException e) {
            log.error("XML marshaling failed for financial transaction", e);
            result = ServiceResult.failure("GW-003", "XML transformation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Financial transaction failed for session: {}", request.sessionId(), e);
            result = ServiceResult.failure("GW-999", "Financial transaction failed");
        }
        auditService.record(AuditLog.Operation.SEND, request.sessionId(), request, result);
        return result;
    }

    private ServiceResult<SendResponseDto> doFinancialTransaction(FinancialTransactionRequest request) throws Exception {
        DataPDU dataPDU = dataPDUTransformer.transformToDataPDU(request);
        String xmlContent = dataPDUTransformer.marshalToXml(dataPDU);
        String signedXmlContent = cryptoManager.signXml(xmlContent);

        log.info("Generated signed XML for financial transaction: {}", signedXmlContent);

        Send soapRequest = new Send();
        soapRequest.setSessionId(request.sessionId());
        Send.Message soapMessage = new Send.Message();
        soapMessage.setBlock4(signedXmlContent);
        soapMessage.setMsgReceiver(rtgsMsgReceiver);
        soapMessage.setMsgSender(request.transaction().senderBic());
        soapMessage.setMsgType("pacs.008.001.08");
        soapMessage.setMsgSequence(request.transaction().msgSequence());
        soapMessage.setFormat("MX");
        soapRequest.setMessage(soapMessage);

        SendResponse response = soapClient.send(soapRequest);

        if (response == null || response.getData() == null) {
            return ServiceResult.failure("GW-002", "Received empty response from Gateway");
        }
        SendResponseData data = response.getData();
        if ("NAK".equals(data.getType())) {
            return ServiceResult.failure(data.getCode(), data.getDescription());
        }
        return ServiceResult.success(SendResponseDto.from(data));
    }

    private Send buildSend(FinancialTransactionRequest request, String signedXml) {
        Send soapRequest = new Send();
        soapRequest.setSessionId(request.sessionId());
        Send.Message msg = new Send.Message();
        msg.setBlock4(signedXml);
        msg.setMsgReceiver(rtgsMsgReceiver);
        msg.setMsgSender(request.transaction().senderBic());
        msg.setMsgType("pacs.008.001.08");
        msg.setMsgSequence(request.transaction().msgSequence());
        msg.setFormat("MX");
        soapRequest.setMessage(msg);
        return soapRequest;
    }

}
