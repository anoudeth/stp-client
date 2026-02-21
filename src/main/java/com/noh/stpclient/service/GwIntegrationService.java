package com.noh.stpclient.service;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.remote.GWClientMuRemote;
import com.noh.stpclient.web.dto.LogonResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for Gateway Integration.
 * Orchestrates calls to the SOAP Client.
 */
@Service
@Slf4j
public class GwIntegrationService {

    private final GWClientMuRemote soapClient;

    public GwIntegrationService(final GWClientMuRemote soapClient) {
        this.soapClient = soapClient;
    }

    /**
     * Authenticates with the Gateway Service.
     *
     * @param username The service username
     * @param password The service password
     * @return The session ID from the response
     * @throws IllegalArgumentException if credentials are empty
     * @throws RuntimeException         if SOAP call fails
     */
    public ServiceResult<LogonResponseDto> performLogon(final String username, final String password) {
        try {
            log.debug("Attempting logon for user: {}", username);
            final var response = soapClient.logon(username, password);

            if (response == null || response.getSessionId() == null) {
                return ServiceResult.failure("unknown.unable.to.process.code", "Received empty session ID from Gateway");
            }
            return ServiceResult.success(new LogonResponseDto(response.getSessionId()));
        } catch (final GatewayIntegrationException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Logon failed for user: {}", username, e);
            throw new RuntimeException("Gateway Logon Failed", e);
        }
    }

    /**
     * Logs out the session.
     * @param sessionId The session to invalidate.
     */
    public void performLogout(final String sessionId) {
        log.debug("Attempting logout for session: {}", sessionId);
        soapClient.logout(sessionId);
    }

    /**
     * Retrieves updates for the given session.
     * @param sessionId The active session ID.
     */
    public void getUpdates(final String sessionId) {
        log.debug("Fetching updates for session: {}", sessionId);
        soapClient.getUpdates(sessionId);
    }

    /**
     * Sends a payload to the gateway.
     * @param payload The data to send.
     */
    public void send(final Object payload) {
        log.debug("Sending payload to gateway");
        soapClient.send(payload);
    }

    /**
     * Sends an ACK or NAK for a specific message.
     * @param messageId The message ID to acknowledge.
     * @param isAck True for ACK, False for NAK.
     */
    public void sendAckNak(final String messageId, final boolean isAck) {
        log.debug("Sending ACK/NAK for msg: {}, isAck: {}", messageId, isAck);
        soapClient.sendAckNak(messageId, isAck);
    }
}
