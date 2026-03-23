package com.noh.stpclient.utils;

import lombok.AllArgsConstructor;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.noh.stpclient.model.xml.SendResponseData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CryptoManager {
    private final JcaSignerInfoGeneratorBuilder builder;
    private final JcaContentSignerBuilder jcaContentSignerBuilder;

    private static final String PROVIDER = "BC";
    private static final String ALGORITHM = "SHA256withRSA";

    @Value("${ks.type}")
    private String ksType;
    @Value("${ks.pass}")
    private String ksPass;
    @Value("${ks.alias}")
    private String keyAlias;
    @Value("${ks.path}")
    private String ksPath;
    @Value("${gw.cert.alias}")
    private String gwCertAlias;



    public CryptoManager() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        try {
            builder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(PROVIDER).build());
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }

        jcaContentSignerBuilder = new JcaContentSignerBuilder(ALGORITHM).setProvider(PROVIDER);
    }

    private KeyStore loadKeystore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        KeyStore privateKS;
        try (InputStream is = new FileInputStream(this.ksPath)) {
            privateKS = KeyStore.getInstance(this.ksType);
            privateKS.load(is, this.ksPass.toCharArray());
        }
        return privateKS;
    }

    public String signValue(String value) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException {

        KeyStore privateKS = loadKeystore();

        X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey) privateKS.getKey(keyAlias, this.ksPass.toCharArray());
        if (privateKey == null) {
            throw new IllegalStateException("Crypto error, failed to load private key " + keyAlias);
        }

        try {
            final byte[] buf = value.getBytes("UTF-16LE");

            CMSTypedData typedData = new CMSProcessableByteArray(buf);
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            ContentSigner signer = jcaContentSignerBuilder.build(privateKey);

            gen.addSignerInfoGenerator(builder.build(signer, certToSign));
            CMSSignedData signed = gen.generate(typedData, false);
            byte[] der = signed.getEncoded();

            return Base64.getEncoder().encodeToString(der);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Signs the {@code <Document>} element inside an MX message XML string using XAdES.
     * The resulting {@code <ds:Signature>} is appended inside the {@code <AppHdr>} element
     * as a child {@code <Sgntr>} node, following the pattern in MXdocument.sign().
     */
    public String signXml(String xmlString) throws Exception {
        final String xadesNS = "http://uri.etsi.org/01903/v1.3.2#";
        final String signedpropsIdSuffix = "-signedprops";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        org.w3c.dom.Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));

        KeyStore privateKS = loadKeystore();
        X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey) privateKS.getKey(keyAlias, this.ksPass.toCharArray());
        if (privateKey == null) {
            throw new IllegalStateException("Crypto error, failed to load private key " + keyAlias);
        }

        XMLSignatureFactory fac;
        try {
            fac = XMLSignatureFactory.getInstance("DOM", "XMLDSig");
        } catch (NoSuchProviderException ex) {
            fac = XMLSignatureFactory.getInstance("DOM");
        }

        // 1. Prepare KeyInfo
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509IssuerSerial x509is = kif.newX509IssuerSerial(
                certToSign.getIssuerX500Principal().toString(),
                certToSign.getSerialNumber());
        X509Data x509data = kif.newX509Data(Collections.singletonList(x509is));
        final String keyInfoId = "_" + UUID.randomUUID().toString();
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509data), keyInfoId);

        // 2. Prepare references
        List<Reference> refs = new ArrayList<>();

        Reference ref1 = fac.newReference("#" + keyInfoId,
                fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(
                        fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (XMLStructure) null)),
                null, null);
        refs.add(ref1);

        final String signedpropsId = "_" + UUID.randomUUID().toString() + signedpropsIdSuffix;

        Reference ref2 = fac.newReference("#" + signedpropsId,
                fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(
                        fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (XMLStructure) null)),
                "http://uri.etsi.org/01903/v1.3.2#SignedProperties", null);
        refs.add(ref2);

        Reference ref3 = fac.newReference(null,
                fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(
                        fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (XMLStructure) null)),
                null, null);
        refs.add(ref3);

        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (XMLStructure) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), refs);

        // 3. Create element AppHdr/Sgntr that will contain the <ds:Signature>
        Node appHdr = null;
        NodeList sgntrList = doc.getElementsByTagName("AppHdr");
        if (sgntrList.getLength() != 0)
            appHdr = sgntrList.item(0);

        if (appHdr == null)
            throw new Exception("mandatory element AppHdr is missing in the document to be signed");

        Node sgntr = appHdr.appendChild(doc.createElementNS(appHdr.getNamespaceURI(), "Sgntr"));

        DOMSignContext dsc = new DOMSignContext(privateKey, sgntr);
        dsc.putNamespacePrefix(XMLSignature.XMLNS, "ds");

        // 4. Set up <ds:Object> with <QualifyingProperties> inside that includes SigningTime
        Element QPElement = doc.createElementNS(xadesNS, "xades:QualifyingProperties");
        QPElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", xadesNS);

        Element SPElement = doc.createElementNS(xadesNS, "xades:SignedProperties");
        SPElement.setAttributeNS(null, "Id", signedpropsId);
        dsc.setIdAttributeNS(SPElement, null, "Id");
        SPElement.setIdAttributeNS(null, "Id", true);
        QPElement.appendChild(SPElement);

        Element SSPElement = doc.createElementNS(xadesNS, "xades:SignedSignatureProperties");
        SPElement.appendChild(SSPElement);

        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String signingTime = df.format(new Date());

        Element STElement = doc.createElementNS(xadesNS, "xades:SigningTime");
        STElement.appendChild(doc.createTextNode(signingTime + "Z"));
        SSPElement.appendChild(STElement);

        DOMStructure qualifPropStruct = new DOMStructure(QPElement);
        List<DOMStructure> xmlObj = new ArrayList<>();
        xmlObj.add(qualifPropStruct);
        XMLObject object = fac.newXMLObject(xmlObj, null, null, null);
        List<XMLObject> objects = Collections.singletonList(object);

        // 5. Set up custom URIDereferencer to process Reference without URI.
        // This Reference points to element <Document> of MX message
        final NodeList docNodes = doc.getElementsByTagName("Document");
        final Node docNode = docNodes.item(0);

        ByteArrayOutputStream refOutputStream = new ByteArrayOutputStream();
        Transformer xform = TransformerFactory.newInstance().newTransformer();
        xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xform.transform(new DOMSource(docNode), new StreamResult(refOutputStream));
        InputStream refInputStream = new ByteArrayInputStream(refOutputStream.toByteArray());
        dsc.setURIDereferencer(new NoUriDereferencer(refInputStream));

        // 6. Sign it!
        XMLSignature signature = fac.newXMLSignature(si, ki, objects, null, null);
        signature.sign(dsc);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        return out.toString(StandardCharsets.UTF_8).replace("&#13;", "");
    }

    /**
     * Verifies the XAdES signature inside a signed DataPDU XML string.
     * Uses the certificate stored under {@code ks.alias} in the keystore.
     */
    public boolean verifyXml(String xmlString) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        org.w3c.dom.Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));

        KeyStore privateKS = loadKeystore();
        final X509Certificate signerCertificate = (X509Certificate) privateKS.getCertificate(keyAlias);

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate(
                "//*[local-name()='Signature']", doc.getDocumentElement(), XPathConstants.NODESET);

        if (nodes == null || nodes.getLength() == 0)
            throw new IllegalStateException("Signature is missing in the document");

        Node nodeSignature = nodes.item(0);

        KeySelector mockKeySelector = new KeySelector() {
            @Override
            public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose,
                                            AlgorithmMethod method, XMLCryptoContext context)
                    throws KeySelectorException {
                return () -> signerCertificate.getPublicKey();
            }
        };

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        NodeList docNodes = doc.getElementsByTagNameNS("*", "Document");
        Node docNode = docNodes.item(0);

        ByteArrayOutputStream refOut = new ByteArrayOutputStream();
        Transformer xform = TransformerFactory.newInstance().newTransformer();
        xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xform.transform(new DOMSource(docNode), new StreamResult(refOut));
        InputStream refInputStream = new ByteArrayInputStream(refOut.toByteArray());

        DOMValidateContext valContext = new DOMValidateContext(mockKeySelector, nodeSignature);
        valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
        valContext.setURIDereferencer(new NoUriDereferencer(refInputStream));

        // Register SignedProperties Id — required since Java 1.7.0_25+ (JDK-8019379)
        NodeList nl = doc.getElementsByTagNameNS("http://uri.etsi.org/01903/v1.3.2#", "SignedProperties");
        if (nl.getLength() == 0)
            throw new IllegalStateException("SignedProperties is missing in signature");

        Element elemSignedProps = (Element) nl.item(0);
        valContext.setIdAttributeNS(elemSignedProps, null, "Id");

        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        return signature.validate(valContext);
    }

    /**
     * Verifies the CMS signature on a send response from the gateway.
     * The gateway signs type+datetime+mir+ref concatenated as UTF-16LE bytes (detached CMS).
     * Mirrors the approach in XmlSigner.verify() but for CMS instead of XML signatures.
     */
    public boolean verifyResponseSignature(SendResponseData data) throws Exception {
        if (data.getSignature() == null || data.getSignature().isBlank()) {
            throw new IllegalArgumentException("Response signature is empty");
        }

        X509Certificate gatewayCert = loadGatewayCert();

        byte[] signatureBytes = Base64.getDecoder().decode(data.getSignature());

        // Gateway signs the concatenation of key response fields, same UTF-16LE encoding as signValue
        String signedContent = nullToEmpty(data.getType())
                + nullToEmpty(data.getDatetime())
                + nullToEmpty(data.getMir())
                + nullToEmpty(data.getRef());
        byte[] contentBytes = signedContent.getBytes("UTF-16LE");

        CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(contentBytes), signatureBytes);

        for (SignerInformation signer : cms.getSignerInfos().getSigners()) {
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(PROVIDER).build(gatewayCert))) {
                return true;
            }
        }
        return false;
    }

    private X509Certificate loadGatewayCert() throws Exception {
        KeyStore ks = loadKeystore();
        X509Certificate cert = (X509Certificate) ks.getCertificate(this.gwCertAlias);
        if (cert == null) {
            throw new IllegalStateException("Gateway certificate not found in keystore with alias: " + this.gwCertAlias);
        }
        return cert;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, UnrecoverableKeyException {

        CryptoManager cryptoManager = new CryptoManager();
        // Set fields manually
        cryptoManager.ksPath = "key/LBBCLALABXXX.pfx";
        cryptoManager.ksType = "PKCS12";
        cryptoManager.ksPass = "2wsx@WSX";
        cryptoManager.keyAlias = "te-c44b72d1-77d0-4664-bb5c-a61eaa6fe971";

        System.out.println(cryptoManager.signValue("lbb@2026"));

    }
}
