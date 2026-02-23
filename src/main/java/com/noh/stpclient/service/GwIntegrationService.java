package com.noh.stpclient.service;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.model.xml.Send;
import com.noh.stpclient.model.xml.SendResponse;
import com.noh.stpclient.model.xml.SendResponseData;
import com.noh.stpclient.remote.GWClientMuRemote;
import com.noh.stpclient.utils.CryptoManager;
import com.noh.stpclient.web.dto.LogonResponseDto;
import com.noh.stpclient.web.dto.SendRequest;
import com.noh.stpclient.web.dto.SendResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Service layer for Gateway Integration.
 * Orchestrates calls to the SOAP Client.
 */
@Service
@Slf4j
public class GwIntegrationService {

    private final GWClientMuRemote soapClient;
    private final CryptoManager cryptoManager;

    @Value("${stp.soap.username}")
    private String stpUsername;
    @Value("${stp.soap.password}")
    private String stpPassword;

    public GwIntegrationService(GWClientMuRemote soapClient,
                                CryptoManager cryptoManager,
                                @Value("${stp.soap.username}") String stpUsername,
                                @Value("${stp.soap.password}") String stpPassword) {
        this.soapClient = soapClient;
        this.cryptoManager = cryptoManager;
        this.stpUsername = stpUsername;
        this.stpPassword = stpPassword;
    }

    public ServiceResult<LogonResponseDto> performLogon() {

        try {
            // sign password
            String signedPassword = cryptoManager.signValue(this.stpPassword);
            log.info("Signed Password: {}", signedPassword);

            final LogonResponse response = soapClient.logon(this.stpUsername, this.stpPassword, signedPassword);

            if (response == null || response.getSessionId() == null) {
                return ServiceResult.failure("GW-001", "Received empty session ID from Gateway");
            }

            String sessionId = response.getSessionId();
            log.info("Received Session ID: {}", sessionId);

            return ServiceResult.success(new LogonResponseDto(sessionId));
        } catch (GatewayIntegrationException e) {
            log.error(e.getMessage());
            return ServiceResult.failure(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("Logon failed for user: {}", this.stpUsername, e);
            return ServiceResult.failure("GW-999", "Gateway Logon Failed");
        }
    }

    public ServiceResult<Void> performLogout(String sessionId) {
        Assert.hasText(sessionId, "Session ID must not be empty");
        try {
            soapClient.logout(sessionId);
            return ServiceResult.success(null);
        } catch (GatewayIntegrationException e) {
            return ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("Logout failed for session: {}", sessionId, e);
            return ServiceResult.failure("GW-999", "Gateway Logout Failed");
        }
    }

    public ServiceResult<Void> performGetUpdates(String sessionId) {
        Assert.hasText(sessionId, "Session ID must not be empty");
        try {
            soapClient.getUpdates(sessionId);
            return ServiceResult.success(null);
        } catch (GatewayIntegrationException e) {
            return ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("GetUpdates failed for session: {}", sessionId, e);
            return ServiceResult.failure("GW-999", "Gateway GetUpdates Failed");
        }
    }

    public ServiceResult<SendResponseDto> performSend(SendRequest request) {
        Assert.notNull(request, "Send request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.notNull(request.message(), "Message content cannot be null");

        try {
            Send soapRequest = new Send();
            soapRequest.setSessionId(request.sessionId());
            Send.Message soapMessage = new Send.Message();
            soapMessage.setBlock4(request.message().block4());
            soapMessage.setMsgReceiver(request.message().msgReceiver());
            soapMessage.setMsgSender(request.message().msgSender());
            soapMessage.setMsgType(request.message().msgType());
            soapMessage.setMsgUserReference(request.message().msgUserReference());
            soapMessage.setFormat(request.message().format());
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
        } catch (GatewayIntegrationException e) {
            return ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("Send failed for session: {}", request.sessionId(), e);
            return ServiceResult.failure("GW-999", "Gateway Send Failed");
        }
    }

    public ServiceResult<Void> performSendAckNak(String messageId, boolean isAck) {
        Assert.hasText(messageId, "Message ID must not be empty");
        try {
            soapClient.sendAckNak(messageId, isAck);
            return ServiceResult.success(null);
        } catch (GatewayIntegrationException e) {
            return ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("SendAckNak failed for message: {}", messageId, e);
            return ServiceResult.failure("GW-999", "Gateway SendAckNak Failed");
        }
    }
}
