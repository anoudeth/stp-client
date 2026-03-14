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
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Client for GWClientMU SOAP Service.
 * Handles low-level SOAP marshalling and transmission.
 */
@Slf4j
public class GWClientMuRemote extends WebServiceGatewaySupport {

    public LogonResponse logon(String username, String password, String signedPassword) {
        log.info("op=soap_logon user={} endpoint={}", username, getWebServiceTemplate().getDefaultUri());

        final Logon request = new Logon();
        request.setUsername(username);
        request.setPassword(password);
        request.setSignature(signedPassword);

        return (LogonResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public LogoutResponse logout(String sessionId) {
        log.info("op=soap_logout session={} endpoint={}", sessionId, getWebServiceTemplate().getDefaultUri());
        final Logout request = new Logout();
        request.setSessionId(sessionId);
        return (LogoutResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public GetUpdatesResponse getUpdates(String sessionId) {
        log.info("op=soap_get_updates session={} endpoint={}", sessionId, getWebServiceTemplate().getDefaultUri());
        final GetUpdates request = new GetUpdates();
        request.setSessionId(sessionId);
        return (GetUpdatesResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    public SendResponse send(Send request) {
        log.info("op=soap_send session={} endpoint={}", request.getSessionId(), getWebServiceTemplate().getDefaultUri());
        return (SendResponse) getWebServiceTemplate().marshalSendAndReceive(request, message -> {
            if (message instanceof SaajSoapMessage saajMsg) {
                try {
                    NodeList nodes = saajMsg.getSaajMessage().getSOAPBody().getElementsByTagName("block4");
                    if (nodes.getLength() > 0) {
                        Element block4Elem = (Element) nodes.item(0);
                        String content = block4Elem.getTextContent();
                        while (block4Elem.hasChildNodes()) {
                            block4Elem.removeChild(block4Elem.getFirstChild());
                        }
                        block4Elem.appendChild(
                                saajMsg.getSaajMessage().getSOAPBody().getOwnerDocument().createCDATASection(content)
                        );
                    }
                } catch (jakarta.xml.soap.SOAPException e) {
                    throw new RuntimeException("Failed to wrap block4 in CDATA", e);
                }
            }
        });
    }

    public void sendAckNak(String sessionId, SendResponseData data) {
        log.info("op=soap_send_ack_nak session={} type={} mir={} endpoint={}", sessionId, data.getType(), data.getMir(), getWebServiceTemplate().getDefaultUri());
        final SendAckNak request = new SendAckNak();
        request.setSessionId(sessionId);
        request.setData(data);
        getWebServiceTemplate().marshalSendAndReceive(request);
    }
}
