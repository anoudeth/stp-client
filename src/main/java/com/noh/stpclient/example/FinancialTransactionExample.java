package com.noh.stpclient.example;

import com.noh.stpclient.model.xml.DataPDU;
import com.noh.stpclient.model.xml.Send;
import com.noh.stpclient.service.DataPDUTransformer;
import com.noh.stpclient.utils.CryptoManager;
import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * Example class demonstrating how to use the financial transaction functionality.
 */
@Slf4j
public class FinancialTransactionExample {

    /**
     * Creates a sample financial transaction request based on the provided XML example.
     */
    public static FinancialTransactionRequest createSampleRequest() {
        var transaction = new FinancialTransactionRequest.TransactionData(
            "2605501332860000",                    // messageId
            "123",                                   // msgSequence
            "2605501332860000",                    // businessMessageId
            "LBBCLALABXXX",                            // senderBic
            "LPDRLALAXATS",                            // receiverBic   // BOL
            "LBBCLALABXXX",                            // instructingAgentBic
            "COEBLALAXXX",                         // instructedAgentBic
            "LBBCLALABXXX",                            // debtorAgentBic
            "USD",                                 // currency
            new BigDecimal("64000"),               // amount
            "2026-02-25",                          // settlementDate
            "MR KHAMCHANH SOULIGNAPHANH",          // debtorName
            "0700000864347",                       // debtorAccount
            "0000010001150512",                    // debtorAgentAccount
            "SOMPHONE SAIYAVONG MR",               // creditorName
            "0961227683628",                       // creditorAccount
            "0000010000200510",                    // creditorAgentAccount
            List.of(" PHOXAY", " ", " PHOXAY", " "), // debtorAddressLines
            "/BNF/",                               // instrForNxtAgt
            "PURCHASE FOR GOODS"                   // remittanceInformation
        );

        return new FinancialTransactionRequest(
            "JSESSIONID=xxxxxx", // This will be replaced by actual session ID
            transaction
        );
    }

    /**
     * Example usage of the financial transaction endpoint.
     * 
     * Request body format:
     * {
     *   "data": {
     *     "transaction": {
     *       "messageId": "2605501332860000",
     *       "msgSequence": "1",
     *       "businessMessageId": "2605501332860000",
     *       "senderBic": "IDCBLALA",
     *       "receiverBic": "LPDRLALA",
     *       "instructingAgentBic": "IDCBLALA",
     *       "instructedAgentBic": "COEBLALAXXX",
     *       "debtorAgentBic": "IDCBLALA",
     *       "currency": "USD",
     *       "amount": 64000,
     *       "settlementDate": "2026-02-25",
     *       "debtorName": "MR KHAMCHANH SOULIGNAPHANH",
     *       "debtorAccount": "0700000864347",
     *       "debtorAgentAccount": "0000010001150512",
     *       "creditorName": "SOMPHONE SAIYAVONG MR",
     *       "creditorAccount": "0961227683628",
     *       "creditorAgentAccount": "0000010000200510",
     *       "debtorAddressLines": [" PHOXAY", " ", " PHOXAY", " "],
     *       "instrForNxtAgt": "/BNF/",
     *       "remittanceInformation": "PURCHASE FOR GOODS"
     *     }
     *   }
     * }
     * 
     * Endpoint: POST /gw/financial-transaction
     * 
     * Response will contain the generated XML in the data field if successful.
     */
    public static void main(String[] args) throws Exception {
        FinancialTransactionRequest request = createSampleRequest();

        DataPDUTransformer transformer = new DataPDUTransformer();
        String xml = transformer.marshalToXml(transformer.transformToDataPDU(request));
        log.info("Generated XML:\n{}", xml);

        CryptoManager cryptoManager = new CryptoManager();
        setField(cryptoManager, "ksPath", "key/LBBCLALABXXX.pfx");
        setField(cryptoManager, "ksType", "PKCS12");
        setField(cryptoManager, "ksPass", "2wsx@WSX");
        setField(cryptoManager, "keyAlias", "te-c44b72d1-77d0-4664-bb5c-a61eaa6fe971");

        String sendXml = buildSendXml(request, cryptoManager.signXml(xml));
        log.info("Send XML:\n{}", sendXml);
    }

    private static String buildSendXml(FinancialTransactionRequest request, String signedXml) throws Exception {
        Send send = new Send();
        send.setSessionId(request.sessionId());
        Send.Message msg = new Send.Message();
        msg.setBlock4(signedXml);
        msg.setMsgReceiver(request.transaction().receiverBic());
        msg.setMsgSender(request.transaction().senderBic());
        msg.setMsgType("pacs.008.001.08");
        msg.setMsgSequence(request.transaction().msgSequence());
        msg.setFormat("MX");
        send.setMessage(msg);

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

    private static void setField(Object obj, String fieldName, String value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
