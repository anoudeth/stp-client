package com.noh.stpclient.service;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.remote.GWClientMuRemote;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Service layer for Gateway Integration.
 * Orchestrates calls to the SOAP Client.
 */
@Service
public class GwIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(GwIntegrationService.class);
    private final GWClientMuRemote soapClient;

    public GwIntegrationService(GWClientMuRemote soapClient) {
        this.soapClient = soapClient;
    }

    /**
     * Authenticates with the Gateway Service.
     *
     * @param username The service username
     * @param password The service password
     * @return The session ID from the response
     * @throws IllegalArgumentException if credentials are empty
     * @throws RuntimeException if SOAP call fails
     */
    public String performLogon(String username, String password) {
        Assert.hasText(username, "Username must not be empty");
        Assert.hasText(password, "Password must not be empty");

        try {
            logger.debug("Attempting logon for user: {}", username);

            final LogonResponse response = soapClient.logon(username, password);

            if (response == null || response.getSessionId() == null) {
                throw new RuntimeException("Received empty session ID from Gateway");
            }

            return response.getSessionId();
        } catch (Exception e) {
            logger.error("Logon failed for user: {}", username, e);
            throw new RuntimeException("Gateway Logon Failed", e);
        }
    }
}