package com.noh.stpclient.remote;

import com.noh.stpclient.model.xml.Logon;
import com.noh.stpclient.model.xml.LogonResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

/**
 * Client for GWClientMU SOAP Service.
 * Handles low-level SOAP marshalling and transmission.
 */
@Slf4j
public class GWClientMuRemote extends WebServiceGatewaySupport {
    
    public LogonResponse logon(String username, String password) {
        log.info("Initiating SOAP Logon for user: {}", username);

        final Logon request = new Logon();
        request.setUsername(username);
        request.setPassword(password);

        // Removed explicit SoapActionCallback as the server rejected the constructed one.
        // Spring Web Services will typically send an empty SOAPAction "" by default,
        // or the server will dispatch based on the request payload body.
        return (LogonResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    // Placeholder for Logout
    public void logout(String sessionId) {
        log.info("Initiating SOAP Logout for session: {}", sessionId);
        // TODO: Implement Logout Request object and call marshalSendAndReceive
        throw new UnsupportedOperationException("Logout not implemented yet");
    }

    // Placeholder for GetUpdates
    public void getUpdates(String sessionId) {
        log.info("Initiating SOAP GetUpdates for session: {}", sessionId);
        // TODO: Implement GetUpdates Request object
        throw new UnsupportedOperationException("GetUpdates not implemented yet");
    }

    // Placeholder for Send
    public void send(Object payload) {
        log.info("Initiating SOAP Send");
        // TODO: Implement Send Request object
        throw new UnsupportedOperationException("Send not implemented yet");
    }

    // Placeholder for SendACKNAK
    public void sendAckNak(String messageId, boolean isAck) {
        log.info("Initiating SOAP SendACKNAK for msg: {}, ack: {}", messageId, isAck);
        // TODO: Implement SendACKNAK Request object
        throw new UnsupportedOperationException("SendACKNAK not implemented yet");
    }
}
