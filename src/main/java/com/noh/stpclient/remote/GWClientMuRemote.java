package com.noh.stpclient.remote;

import com.noh.stpclient.model.xml.GetUpdates;
import com.noh.stpclient.model.xml.Logon;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.model.xml.Logout;
import com.noh.stpclient.model.xml.Send;
import com.noh.stpclient.model.xml.SendAckNak;
import lombok.extern.slf4j.Slf4j;
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

        return (LogonResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public void logout(String sessionId) {
        log.info("Initiating SOAP Logout for session: {}", sessionId);
        final Logout request = new Logout();
        request.setSessionId(sessionId);
        getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public void getUpdates(String sessionId) {
        log.info("Initiating SOAP GetUpdates for session: {}", sessionId);
        final GetUpdates request = new GetUpdates();
        request.setSessionId(sessionId);
        getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public void send(Send request) {
        log.info("Initiating SOAP Send for session: {}", request.getSessionId());
        getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public void sendAckNak(String messageId, boolean isAck) {
        log.info("Initiating SOAP SendACKNAK for msg: {}, ack: {}", messageId, isAck);
        final SendAckNak request = new SendAckNak();
        request.setMessageId(messageId);
        request.setAck(isAck);
        getWebServiceTemplate().marshalSendAndReceive(request);
    }
}
