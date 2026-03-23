package com.noh.stpclient.utils;

import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.security.auth.x500.X500Principal;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoManagerXmlSignTest {

    @TempDir
    Path tempDir;

    private CryptoManager cryptoManager;

    private static final String ALIAS   = "test-key";
    private static final String KS_PASS = "testpass";

    // Minimal DataPDU XML matching the structure produced by DataPDUTransformer
    private static final String SAMPLE_XML =
        "<DataPDU xmlns=\"urn:cma:stp:xsd:stp.1.0\">" +
        "<Body>" +
        "<AppHdr xmlns=\"urn:iso:std:iso:20022:tech:xsd:head.001.001.01\">" +
            "<Fr><FIId><FinInstnId><BICFI>LBBCLABB</BICFI></FinInstnId></FIId></Fr>" +
            "<To><FIId><FinInstnId><BICFI>BCELLAOL</BICFI></FinInstnId></FIId></To>" +
            "<BizMsgIdr>TEST-001</BizMsgIdr>" +
            "<MsgDefIdr>pacs.008.001.08</MsgDefIdr>" +
            "<BizSvc>RTGS</BizSvc>" +
            "<CreDt>2026-03-23T08:00:00.000Z</CreDt>" +
        "</AppHdr>" +
        "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">" +
            "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                    "<MsgId>MSG-001</MsgId>" +
                    "<CreDtTm>2026-03-23T08:00:00</CreDtTm>" +
                    "<NbOfTxs>1</NbOfTxs>" +
                    "<SttlmInf><SttlmMtd>CLRG</SttlmMtd></SttlmInf>" +
                "</GrpHdr>" +
                "<CdtTrfTxInf>" +
                    "<PmtId><InstrId>INSTR-001</InstrId><EndToEndId>E2E-001</EndToEndId><TxId>TX-001</TxId></PmtId>" +
                    "<IntrBkSttlmAmt Ccy=\"LAK\">1000000</IntrBkSttlmAmt>" +
                    "<ChrgBr>SHAR</ChrgBr>" +
                    "<Dbtr><Nm>Test Debtor</Nm></Dbtr>" +
                    "<Cdtr><Nm>Test Creditor</Nm></Cdtr>" +
                "</CdtTrfTxInf>" +
            "</FIToFICstmrCdtTrf>" +
        "</Document>" +
        "</Body>" +
        "</DataPDU>";

    @BeforeEach
    void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Generate RSA keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        // Self-signed certificate
        X500Principal subject = new X500Principal("CN=TestSigner, O=Test, C=LA");
        Date notBefore = new Date();
        Date notAfter  = new Date(notBefore.getTime() + 365L * 24 * 60 * 60 * 1000);

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(
                    new JcaX509v3CertificateBuilder(
                        subject, BigInteger.ONE, notBefore, notAfter, subject, keyPair.getPublic()
                    ).build(new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.getPrivate()))
                );

        // Write temp PKCS12 keystore
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(ALIAS, keyPair.getPrivate(), KS_PASS.toCharArray(), new X509Certificate[]{cert});

        Path ksFile = tempDir.resolve("test.p12");
        try (FileOutputStream fos = new FileOutputStream(ksFile.toFile())) {
            ks.store(fos, KS_PASS.toCharArray());
        }

        cryptoManager = new CryptoManager();
        ReflectionTestUtils.setField(cryptoManager, "ksPath",    ksFile.toString());
        ReflectionTestUtils.setField(cryptoManager, "ksType",    "PKCS12");
        ReflectionTestUtils.setField(cryptoManager, "ksPass",    KS_PASS);
        ReflectionTestUtils.setField(cryptoManager, "keyAlias",  ALIAS);
        ReflectionTestUtils.setField(cryptoManager, "gwCertAlias", ALIAS);
    }

    @Test
    void signXml_thenVerify_shouldPass() throws Exception {
        String signedXml = cryptoManager.signXml(SAMPLE_XML);

        assertNotNull(signedXml, "signXml should return non-null");
        assertTrue(signedXml.contains("ds:Signature"), "Signed XML must contain ds:Signature");
        assertTrue(signedXml.contains("Sgntr"), "Signed XML must contain Sgntr element in AppHdr");

        boolean valid = cryptoManager.verifyXml(signedXml);
        assertTrue(valid, "XAdES signature verification failed");
    }
}