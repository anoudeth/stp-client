package com.noh.stpclient;

import com.noh.stpclient.utils.CryptoManager;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Signs the saved raw XML file using CryptoManager and writes the output for inspection.
 * Used to investigate BOL "verify Signature failed" by isolating sign path from XML generation.
 */
class CryptoManagerSignXmlTest {

    private static final String RAW_XML_PATH = "xmlfile/pacs009_RTGS.xml";
    private static final String OUTPUT_DIR    = "xmlfile";

    @Test
    void signRawXmlFileAndSave() throws Exception {
        CryptoManager crypto = buildCryptoManager();

        File xmlFile = new File(RAW_XML_PATH);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder dBuilder = factory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);

        String signedXml = crypto.signXml(doc);
        assertNotNull(signedXml, "signXml returned null");

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String outPath = OUTPUT_DIR + "/signed2_" + ts + ".xml";
        Files.writeString(Paths.get(outPath), signedXml);
        System.out.println("[TEST] Signed XML saved to: " + outPath);
        System.out.println("[TEST] Signed XML length: " + signedXml.length());

        assertTrue(signedXml.contains("Sgntr"), "Signed XML should contain Sgntr");
        assertTrue(signedXml.contains("ds:Signature"), "Signed XML should contain ds:Signature");
    }

    private CryptoManager buildCryptoManager() throws Exception {
        CryptoManager crypto = new CryptoManager();
        setField(crypto, "ksPath",    "key/LBBCLALABXXX.pfx");
        setField(crypto, "ksType",    "PKCS12");
        setField(crypto, "ksPass",    "2wsx@WSX");
        setField(crypto, "keyAlias",  "te-c44b72d1-77d0-4664-bb5c-a61eaa6fe971");
        return crypto;
    }

    private void setField(Object target, String name, String value) throws Exception {
        Field f = CryptoManager.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}