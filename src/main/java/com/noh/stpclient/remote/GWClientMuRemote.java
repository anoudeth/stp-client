package com.noh.stpclient.remote;

import com.noh.stpclient.model.xml.GetUpdates;
import com.noh.stpclient.model.xml.GetUpdatesResponse;
import com.noh.stpclient.model.xml.Logon;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.model.xml.Logout;
import com.noh.stpclient.model.xml.LogoutResponse;
import com.noh.stpclient.model.xml.Send;
import com.noh.stpclient.model.xml.SendAckNak;
import com.noh.stpclient.model.xml.SendResponse;
import com.noh.stpclient.model.xml.SendResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

/**
 * Client for GWClientMU SOAP Service.
 * Handles low-level SOAP marshalling and transmission.
 */
@Slf4j
public class GWClientMuRemote extends WebServiceGatewaySupport {

    public LogonResponse logon(String username, String password, String signedPassword) {
        log.info(":: Initiating SOAP Logon for user: {}", username);

        final Logon request = new Logon();
        request.setUsername(username);
        request.setPassword(password);
        request.setSignature(signedPassword);

        return (LogonResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public LogoutResponse logout(String sessionId) {
        log.info(":: Initiating SOAP Logout for session: {}", sessionId);
        final Logout request = new Logout();
        request.setSessionId(sessionId);
        return (LogoutResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public GetUpdatesResponse getUpdates(String sessionId) {
        log.info(":: Initiating SOAP GetUpdates for session: {}", sessionId);
        final GetUpdates request = new GetUpdates();
        request.setSessionId(sessionId);
        return (GetUpdatesResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public SendResponse send(Send request) {
        log.info(":: Initiating SOAP Send for session: {}", request.getSessionId());
        return (SendResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public void sendAckNak(String sessionId, SendResponseData data) {
        log.info(":: Initiating SOAP SendACKNAK for session: {}, type: {}, mir: {}", sessionId, data.getType(), data.getMir());
        final SendAckNak request = new SendAckNak();
        request.setSessionId(sessionId);
        request.setData(data);
        getWebServiceTemplate().marshalSendAndReceive(request);
    }
}
