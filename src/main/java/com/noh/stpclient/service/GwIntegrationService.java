package com.noh.stpclient.service;

import com.noh.stpclient.audit.AuditLog;
import com.noh.stpclient.audit.AuditRepository;
import com.noh.stpclient.audit.AuditService;
import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.model.xml.GetUpdatesItem;
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
import com.noh.stpclient.web.dto.SendXmlFileRequest;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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
    private final AuditRepository auditRepository;
    private final SessionManager sessionManager;

    private static final String MSG_FORMAT = "MX";

    @Value("${stp.soap.msg-receiver}")
    private String msgReceiver;
    @Value("${stp.soap.msg-sender}")
    private String msgSender;

    @Value("${stp.xml.output-dir:xmlfile}")
    private String xmlOutputDir;

    public GwIntegrationService(GWClientMuRemote soapClient,
                                CryptoManager cryptoManager,
                                DataPDUTransformer dataPDUTransformer,
                                AuditService auditService,
                                AuditRepository auditRepository,
                                SessionManager sessionManager,
                                @Value("${stp.soap.msg-receiver}") String msgReceiver,
                                @Value("${stp.soap.msg-sender}") String msgSender) {
        this.soapClient = soapClient;
        this.cryptoManager = cryptoManager;
        this.dataPDUTransformer = dataPDUTransformer;
        this.auditService = auditService;
        this.auditRepository = auditRepository;
        this.sessionManager = sessionManager;
        this.msgReceiver = msgReceiver;
        this.msgSender = msgSender;
    }

    public ServiceResult<LogonResponseDto> performLogon() {
        try {
            String sessionId = sessionManager.getOrCreate();
            log.info("Session ID: {}", sessionId);
            return ServiceResult.success(new LogonResponseDto(sessionId));
        } catch (GatewayIntegrationException e) {
            log.warn("performLogon failed | code={} desc={}", e.getCode(), e.getDescription());
            return ServiceResult.failure(e.getCode(), e.getDescription(), e.getInfo());
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
            log.warn("performLogout failed | code={} desc={}", e.getCode(), e.getDescription());
            return ServiceResult.failure(e.getCode(), e.getDescription(), e.getInfo());
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
                    result = ServiceResult.failure(retryEx.getCode(), retryEx.getDescription(), retryEx.getInfo());
                } catch (Exception retryEx) {
                    log.error("GetUpdates retry failed for session: {}", freshSession, retryEx);
                    result = ServiceResult.failure("GW-999", "Gateway GetUpdates Failed");
                }
            } else {
                log.warn("GetUpdates failed | sessionId={} code={} desc={}", sessionId, e.getCode(), e.getDescription());
                result = ServiceResult.failure(e.getCode(), e.getDescription(), e.getInfo());
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
        List<GetUpdatesItem> rawItems = response.getItems();
        List<GetUpdatesResponseDto> dtos = rawItems.stream()
                .map(GetUpdatesResponseDto::from)
                .toList();
        log.info("GetUpdates OK | items={}", dtos.size());
        saveGetUpdateItems(rawItems);
        return ServiceResult.success(dtos);
    }

    private void saveGetUpdateItems(List<GetUpdatesItem> items) {
        for (GetUpdatesItem item : items) {
            try {
                String msgId = extractMsgId(item.getBlock4());
                auditRepository.insertGetUpdateItem(
                        item.getMsgType(), msgId,
                        item.getMsgSender(), item.getMsgReceiver(), item.getMsgFormat(),
                        item.getMsgSubFormat(), item.getFormat(), item.getMsgSession(),
                        item.getMsgSequence(), item.getMsgPriority(), item.getMsgUserPriority(),
                        item.getMsgUserReference(), item.getMsgNetMir(), item.getMsgNetInputTime(),
                        item.getMsgNetOutputDate(), item.getMsgPacResult(), item.getMsgFinValidation(),
                        item.getMsgPde(), item.getMsgPdm(), item.getMsgCopySrvId(),
                        item.getMsgCopySrvInfo(), item.getMsgDelNotifRq(), item.getBlock4());
                log.info("Saved STP_GET_UPDATE | msgType={} mir={} msgId={}", item.getMsgType(), item.getMsgNetMir(), msgId);
            } catch (Exception e) {
                log.error("Failed to save STP_GET_UPDATE | mir={}: {}", item.getMsgNetMir(), e.getMessage());
            }
        }
    }

    private String extractMsgId(String block4) {
        if (block4 == null || block4.isBlank()) return null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(block4)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            // Try GrpHdr/MsgId (camt.052/053/054/025/029) then MsgHdr/MsgId (camt.010/019/034/998)
            String ref = (String) xpath.evaluate(
                    "(//*[local-name()='GrpHdr']/*[local-name()='MsgId'])[1]", doc, XPathConstants.STRING);
            if (ref == null || ref.isBlank()) {
                ref = (String) xpath.evaluate(
                        "(//*[local-name()='MsgHdr']/*[local-name()='MsgId'])[1]", doc, XPathConstants.STRING);
            }
            return (ref != null && !ref.isBlank()) ? ref : null;
        } catch (Exception e) {
            log.warn("extractMsgId failed: {}", e.getMessage());
            return null;
        }
    }

    public ServiceResult<SendResponseDto> performSend(SendRequest request) {
        Assert.notNull(request, "Send request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.notNull(request.message(), "Message content cannot be null");

        long start = System.currentTimeMillis();
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
            log.warn("performSend failed | sessionId={} code={} desc={}", request.sessionId(), e.getCode(), e.getDescription());
            result = ServiceResult.failure(e.getCode(), e.getDescription(), e.getInfo());
        } catch (Exception e) {
            log.error("Send failed for session: {}", request.sessionId(), e);
            result = ServiceResult.failure("GW-999", "Gateway Send Failed");
        }
        log.info("performSend | sessionId={} success={} duration_ms={}", request.sessionId(), result.isSuccess(), System.currentTimeMillis() - start);
        auditService.record(AuditLog.Operation.SEND, request.sessionId(), request, result);
        return result;
    }

    public ServiceResult<Void> performSendAckNak(com.noh.stpclient.web.dto.SendAckNakRequest request) {
        Assert.notNull(request, "SendAckNak request cannot be null");
        Assert.hasText(request.type(), "Type must not be empty");
        Assert.hasText(request.datetime(), "Datetime must not be empty");
        Assert.hasText(request.mir(), "MIR must not be empty");

        String sessionId = sessionManager.getOrCreate();
        ServiceResult<Void> result;
        try {
            SendResponseData data = new SendResponseData();
            data.setType(request.type());
            data.setDatetime(request.datetime());
            data.setMir(request.mir());
            soapClient.sendAckNak(sessionId, data);
            result = ServiceResult.success(null);
        } catch (GatewayIntegrationException e) {
            log.warn("performSendAckNak failed | mir={} code={} desc={}", request.mir(), e.getCode(), e.getDescription());
            result = ServiceResult.failure(e.getCode(), e.getDescription(), e.getInfo());
        } catch (Exception e) {
            log.error("SendAckNak failed for mir: {}", request.mir(), e);
            result = ServiceResult.failure("GW-999", "Gateway SendAckNak Failed");
        }
        if (result.isSuccess()) {
            log.info("performSendAckNak OK | sessionId={} mir={} type={}", sessionId, request.mir(), request.type());
        }
        auditService.record(AuditLog.Operation.SEND_ACK_NAK, sessionId, request, result);
        return result;
    }

    public ServiceResult<SendResponseDto> performSendFinancialTransaction(FinancialTransactionRequest request) {
        Assert.notNull(request, "Financial transaction request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.notNull(request.transaction(), "Transaction data cannot be null");

        // 1. Insert PENDING audit row + request fields before SOAP call
        long auditLogId = auditService.recordSendBefore(request.sessionId(), request);

        long start = System.currentTimeMillis();
        ServiceResult<SendResponseDto> result;
        try {
            // Build and sign XML once — not repeated on session-expiry retry
            Object dataPDU = dataPDUTransformer.transformToDataPDU(request);
            String xmlContent = dataPDUTransformer.marshalToXml(dataPDU, request.transaction().msgType());
            String msgId = request.transaction().messageId();
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
            saveXmlFile(msgId + "_" + ts + "_raw.xml", xmlContent);
            String signedXml = cryptoManager.signXml(xmlContent);
            saveXmlFile(msgId + "_" + ts + "_signed.xml", signedXml);

            try {
                result = doSendFinancialTransaction(request, signedXml);
            } catch (GatewayIntegrationException e) {
                if (sessionManager.isSessionExpired(e)) {
                    log.warn("Session {} expired during Send — refreshing and retrying", request.sessionId());
                    sessionManager.invalidate(request.sessionId());
                    String freshSession = sessionManager.getOrCreate();
                    FinancialTransactionRequest retryRequest = new FinancialTransactionRequest(freshSession, request.transaction());
                    try {
                        result = doSendFinancialTransaction(retryRequest, signedXml);
                    } catch (GatewayIntegrationException retryEx) {
                        result = ServiceResult.failure(retryEx.getCode(), retryEx.getDescription(), retryEx.getInfo());
                    } catch (Exception retryEx) {
                        log.error("Send retry failed for session: {}", freshSession, retryEx);
                        result = ServiceResult.failure("GW-999", "Financial transaction send failed");
                    }
                } else {
                    log.warn("performSendFinancialTransaction failed | sessionId={} code={} desc={}", request.sessionId(), e.getCode(), e.getDescription());
                    result = ServiceResult.failure(e.getCode(), e.getDescription(), e.getInfo());
                }
            }
        } catch (JAXBException e) {
            log.error("XML marshaling failed for financial transaction", e);
            result = ServiceResult.failure("GW-003", "XML transformation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Financial transaction send failed for session: {}", request.sessionId(), e);
            result = ServiceResult.failure("GW-999", "Financial transaction send failed");
        }

        log.info("performSendFinancialTransaction | sessionId={} success={} duration_ms={}", request.sessionId(), result.isSuccess(), System.currentTimeMillis() - start);
        // 2. Update audit row with result (async — does not block logout)
        auditService.recordSendAfter(auditLogId, result);
        return result;
    }

    private ServiceResult<SendResponseDto> doSendFinancialTransaction(FinancialTransactionRequest request, String signedXml) throws GatewayIntegrationException {
        Send soapRequest = buildSend(request, signedXml);
        log.debug("Sending SOAP request block4:\n{}", signedXml);
        return sendAndParseResponse(soapRequest);
    }

    private void saveXmlFile(String filename, String content) {
        try {
            Path dir = Paths.get(xmlOutputDir);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(filename), content);
            log.debug("Saved XML file: {}/{}", xmlOutputDir, filename);
        } catch (Exception e) {
            log.warn("Failed to save XML file {}: {}", filename, e.getMessage());
        }
    }

    /**
     * Reads a raw XML file from xmlOutputDir, signs it with XAdES, and sends to the gateway.
     * Useful for investigating signature failures: bypasses XML generation to isolate sign/send path.
     */
    public ServiceResult<SendResponseDto> performSendFromXmlFile(String sessionId, SendXmlFileRequest request) {
        Assert.hasText(sessionId, "Session ID must not be empty");
        Assert.notNull(request, "SendXmlFileRequest cannot be null");

        long start = System.currentTimeMillis();
        ServiceResult<SendResponseDto> result;
        try {
            Path filePath = Paths.get(xmlOutputDir).resolve(request.filename());
            String xmlContent = Files.readString(filePath);
            log.info("performSendFromXmlFile | file={} size={}", request.filename(), xmlContent.length());

            String signedXml = cryptoManager.signXml(xmlContent);
            String signedFilename = request.filename().replaceFirst("\\.xml$", "") + "_signed.xml";
            saveXmlFile(signedFilename, signedXml);

            Send soapRequest = new Send();
            soapRequest.setSessionId(sessionId);
            Send.Message msg = new Send.Message();
            msg.setBlock4(signedXml);
            msg.setMsgReceiver(this.msgReceiver);
            msg.setMsgSender(this.msgSender);
            msg.setMsgType(request.msgType());
            msg.setMsgSequence(request.msgSequence());
            msg.setFormat(MSG_FORMAT);
            soapRequest.setMessage(msg);

            try {
                result = sendAndParseResponse(soapRequest);
            } catch (GatewayIntegrationException e) {
                if (sessionManager.isSessionExpired(e)) {
                    log.warn("Session {} expired during SendXmlFile — refreshing and retrying", sessionId);
                    sessionManager.invalidate(sessionId);
                    String freshSession = sessionManager.getOrCreate();
                    soapRequest.setSessionId(freshSession);
                    try {
                        result = sendAndParseResponse(soapRequest);
                    } catch (GatewayIntegrationException retryEx) {
                        result = ServiceResult.failure(retryEx.getCode(), retryEx.getDescription(), retryEx.getInfo());
                    } catch (Exception retryEx) {
                        log.error("SendXmlFile retry failed: {}", retryEx.getMessage());
                        result = ServiceResult.failure("GW-999", "Send from XML file failed");
                    }
                } else {
                    result = ServiceResult.failure(e.getCode(), e.getDescription(), e.getInfo());
                }
            }
        } catch (Exception e) {
            log.error("Send from XML file failed for session: {}", sessionId, e);
            result = ServiceResult.failure("GW-999", "Send from XML file failed: " + e.getMessage());
        }

        log.info("performSendFromXmlFile | sessionId={} success={} duration_ms={}", sessionId, result.isSuccess(), System.currentTimeMillis() - start);
        return result;
    }

    private ServiceResult<SendResponseDto> sendAndParseResponse(Send soapRequest) throws GatewayIntegrationException {
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

    /**
     * Assembles the SOAP Send request from the transaction request and the XAdES-signed XML.
     * block4 carries the signed MX document; receiver is the configured RTGS endpoint.
     */
    private Send buildSend(FinancialTransactionRequest request, String signedXml) {
        Send soapRequest = new Send();
        soapRequest.setSessionId(request.sessionId());
        Send.Message msg = new Send.Message();
        msg.setBlock4(signedXml);
        msg.setMsgReceiver(this.msgReceiver);
        msg.setMsgSender(this.msgSender);
        msg.setMsgType(request.transaction().msgType());
        msg.setMsgSequence(request.transaction().msgSequence());
        msg.setFormat(MSG_FORMAT);
        soapRequest.setMessage(msg);
        return soapRequest;
    }

}
