package com.noh.stpclient.service;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.model.xml.DataPDU;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.model.xml.Send;
import com.noh.stpclient.model.xml.SendResponse;
import com.noh.stpclient.model.xml.SendResponseData;
import com.noh.stpclient.remote.GWClientMuRemote;
import com.noh.stpclient.utils.CryptoManager;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import com.noh.stpclient.web.dto.LogonResponseDto;
import com.noh.stpclient.web.dto.SendRequest;
import com.noh.stpclient.web.dto.SendResponseDto;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

import jakarta.xml.bind.JAXBException;

/**
 * Service layer for Gateway Integration.
 * Orchestrates calls to the SOAP Client.
 */
@Service
@Slf4j
public class GwIntegrationService {

    private final GWClientMuRemote soapClient;
    private final CryptoManager cryptoManager;
    private final DataPDUTransformer dataPDUTransformer;

    @Value("${stp.soap.username}")
    private String stpUsername;
    @Value("${stp.soap.password}")
    private String stpPassword;

    public GwIntegrationService(GWClientMuRemote soapClient,
                                CryptoManager cryptoManager,
                                DataPDUTransformer dataPDUTransformer,
                                @Value("${stp.soap.username}") String stpUsername,
                                @Value("${stp.soap.password}") String stpPassword) {
        this.soapClient = soapClient;
        this.cryptoManager = cryptoManager;
        this.dataPDUTransformer = dataPDUTransformer;
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
            soapMessage.setMsgSequence(request.message().msgSequence());
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

    public ServiceResult<String> performSendFinancialTransaction(FinancialTransactionRequest request) {
        Assert.notNull(request, "Financial transaction request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.notNull(request.transaction(), "Transaction data cannot be null");

        try {
            DataPDU dataPDU = dataPDUTransformer.transformToDataPDU(request);
            String xmlContent = dataPDUTransformer.marshalToXml(dataPDU);
            String signedXml = cryptoManager.signXml(xmlContent);

            Send soapRequest = buildSend(request, signedXml);
            String sendXml = buildSendXml(soapRequest);
            log.info("Sending SOAP request:\n{}", sendXml);

            SendResponse response = soapClient.send(soapRequest);

            if (response == null || response.getData() == null) {
                return ServiceResult.failure("GW-002", "Received empty response from Gateway");
            }

            SendResponseData data = response.getData();
            if ("NAK".equals(data.getType())) {
                return ServiceResult.failure(data.getCode(), data.getDescription());
            }

            return ServiceResult.success(sendXml);
        } catch (JAXBException e) {
            log.error("XML marshaling failed for financial transaction", e);
            return ServiceResult.failure("GW-003", "XML transformation failed: " + e.getMessage());
        } catch (GatewayIntegrationException e) {
            return ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("Financial transaction send failed for session: {}", request.sessionId(), e);
            return ServiceResult.failure("GW-999", "Financial transaction send failed");
        }
    }

    private Send buildSend(FinancialTransactionRequest request, String signedXml) {
        Send soapRequest = new Send();
        soapRequest.setSessionId(request.sessionId());
        Send.Message msg = new Send.Message();
        msg.setBlock4(signedXml);
        msg.setMsgReceiver(request.transaction().receiverBic());
        msg.setMsgSender(request.transaction().senderBic());
        msg.setMsgType("pacs.008.001.08");
        msg.setMsgSequence(request.transaction().msgSequence());
        msg.setFormat("MX");
        soapRequest.setMessage(msg);
        return soapRequest;
    }

    private String buildSendXml(Send send) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();
        Marshaller marshaller = JAXBContext.newInstance(Send.class).createMarshaller();
        marshaller.marshal(send, doc);

        NodeList block4Nodes = doc.getElementsByTagName("block4");
        if (block4Nodes.getLength() > 0) {
            Element block4Elem = (Element) block4Nodes.item(0);
            String content = block4Elem.getTextContent();
            while (block4Elem.hasChildNodes()) {
                block4Elem.removeChild(block4Elem.getFirstChild());
            }
            block4Elem.appendChild(doc.createCDATASection(content));
        }

        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter sw = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    public ServiceResult<String> performFinancialTransaction(FinancialTransactionRequest request) {
        Assert.notNull(request, "Financial transaction request cannot be null");
        Assert.hasText(request.sessionId(), "Session ID must not be empty");
        Assert.notNull(request.transaction(), "Transaction data cannot be null");

        try {
            // Transform transaction data to DataPDU format
            DataPDU dataPDU = dataPDUTransformer.transformToDataPDU(request);
            
            // Marshal DataPDU to XML string
            String xmlContent = dataPDUTransformer.marshalToXml(dataPDU);

            // Sign the <Document> element and embed <ds:Signature> inside <AppHdr/Sgntr>
            String signedXmlContent = cryptoManager.signXml(xmlContent);

            log.info("Generated signed XML for financial transaction: {}", signedXmlContent);

            // Create Send request with the XML content
            Send soapRequest = new Send();
            soapRequest.setSessionId(request.sessionId());
            Send.Message soapMessage = new Send.Message();
            soapMessage.setBlock4(signedXmlContent);
            soapMessage.setMsgReceiver(request.transaction().receiverBic());
            soapMessage.setMsgSender(request.transaction().senderBic());
            soapMessage.setMsgType("pacs.008.001.08");
            soapMessage.setMsgSequence(request.transaction().msgSequence());
            soapMessage.setFormat("MX");
            soapRequest.setMessage(soapMessage);

            SendResponse response = soapClient.send(soapRequest);
            
            if (response == null || response.getData() == null) {
                return ServiceResult.failure("GW-002", "Received empty response from Gateway");
            }
            
            SendResponseData data = response.getData();
            if ("NAK".equals(data.getType())) {
                return ServiceResult.failure(data.getCode(), data.getDescription());
            }

            return ServiceResult.success(signedXmlContent);
        } catch (JAXBException e) {
            log.error("XML marshaling failed for financial transaction", e);
            return ServiceResult.failure("GW-003", "XML transformation failed: " + e.getMessage());
        } catch (GatewayIntegrationException e) {
            return ServiceResult.failure(e.getCode(), e.getDescription());
        } catch (Exception e) {
            log.error("Financial transaction failed for session: {}", request.sessionId(), e);
            return ServiceResult.failure("GW-999", "Financial transaction failed");
        }
    }
}
