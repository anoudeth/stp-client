package com.noh.stpclient.remote;

import com.noh.stpclient.model.xml.Logon;
import com.noh.stpclient.model.xml.LogonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;

/**
 * Client for GWClientMU SOAP Service.
 * Handles low-level SOAP marshalling and transmission.
 */
public class GWClientMuRemote extends WebServiceGatewaySupport {

    private static final Logger logger = LoggerFactory.getLogger(GWClientMuRemote.class);

    // Namespace matches the 'xmlns:int' in your sample request
    private static final String NAMESPACE = "http://integration.gwclient.smallsystems.cma.se/";

    public LogonResponse logon(String username, String password) {
        logger.info("Initiating SOAP Logon for user: {}", username);

        final Logon request = new Logon();
        request.setUsername(username);
        request.setPassword(password);

        // The SoapActionCallback is often optional depending on the server,
        // but good practice to include if the WSDL defines specific actions.
        // Assuming action pattern based on namespace + operation.
        final SoapActionCallback actionCallback = new SoapActionCallback(NAMESPACE + "logon");

        return (LogonResponse) getWebServiceTemplate()
                .marshalSendAndReceive(request, actionCallback);
    }

    // Placeholder for Logout
    public void logout(String sessionId) {
        logger.info("Initiating SOAP Logout for session: {}", sessionId);
        // TODO: Implement Logout Request object and call marshalSendAndReceive
        throw new UnsupportedOperationException("Logout not implemented yet");
    }

    // Placeholder for GetUpdates
    public void getUpdates(String sessionId) {
        logger.info("Initiating SOAP GetUpdates for session: {}", sessionId);
        // TODO: Implement GetUpdates Request object
        throw new UnsupportedOperationException("GetUpdates not implemented yet");
    }

    // Placeholder for Send
    public void send(Object payload) {
        logger.info("Initiating SOAP Send");
        // TODO: Implement Send Request object
        throw new UnsupportedOperationException("Send not implemented yet");
    }

    // Placeholder for SendACKNAK
    public void sendAckNak(String messageId, boolean isAck) {
        logger.info("Initiating SOAP SendACKNAK for msg: {}, ack: {}", messageId, isAck);
        // TODO: Implement SendACKNAK Request object
        throw new UnsupportedOperationException("SendACKNAK not implemented yet");
    }
}
