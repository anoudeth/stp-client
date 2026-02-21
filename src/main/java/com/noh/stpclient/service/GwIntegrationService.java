package com.noh.stpclient.service;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.remote.GWClientMuRemote;
import com.noh.stpclient.web.dto.LogonResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * @throws RuntimeException         if SOAP call fails
     */
    public ServiceResult<LogonResponseDto> performLogon(String username, String password) {

        try {
            log.debug("Attempting logon for user: {}", username);

            final LogonResponse response = soapClient.logon(username, password);

            if (response == null || response.getSessionId() == null) {
                return ServiceResult.failure("unknown.unable.to.process.code", "Received empty session ID from Gateway");
            }

            LogonResponseDto output = new LogonResponseDto(response.getSessionId());

            return ServiceResult.success(output);

        } catch (GatewayIntegrationException e) {
//            log.error("GWIntegrationService failed to perform logon for user {}", username, e);
//            return ServiceResult.failure("unknown.unable.to.process.code", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Logon failed for user: {}", username, e);
//            return ServiceResult.failure("unknown.unable.to.process.code", e.getMessage());
            throw new RuntimeException("Gateway Logon Failed", e);
        }
    }
}
