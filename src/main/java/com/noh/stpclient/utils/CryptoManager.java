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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;
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
    private String signedpropsIdSuffix;


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
     * as a child {@code <Sgntr>} node, following the pattern in the sample XmlSigner.
     */
    public String signXml(String xmlString) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        return signXml(doc);
    }

    public String signXml(Document doc) throws Exception {
        KeyStore privateKS = loadKeystore();
        X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey) privateKS.getKey(keyAlias, this.ksPass.toCharArray());
        if (privateKey == null) {
            throw new IllegalStateException("Crypto error, failed to load private key " + keyAlias);
        }

        Document signedDoc = signDocument(doc, certToSign, privateKey);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(signedDoc), new StreamResult(out));
        return out.toString(StandardCharsets.UTF_8);
    }

    private Document signDocument(Document doc, X509Certificate signerCertificate, PrivateKey privateKey) throws Exception {
        final String xadesNS = "http://uri.etsi.org/01903/v1.3.2#";
        final String signedpropsIdSuffix = "-signedprops";
        XMLSignatureFactory fac = null;
        try {
            fac = XMLSignatureFactory.getInstance("DOM", "XMLDSig");
        } catch (NoSuchProviderException ex) {
            fac = XMLSignatureFactory.getInstance("DOM");
        }

        // 1. Prepare KeyInfo
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509IssuerSerial x509is = kif.newX509IssuerSerial(signerCertificate.getIssuerX500Principal().toString(),
                signerCertificate.getSerialNumber());
        X509Data x509data = kif.newX509Data(Collections.singletonList(x509is));
        final String keyInfoId = "_" + UUID.randomUUID().toString();
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509data), keyInfoId);

        // 2. Prepare references
        List<Reference> refs = new ArrayList<Reference>();

        Reference ref1 = fac.newReference("#" + keyInfoId, fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(
                        fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (XMLStructure) null)),
                null, null);
        refs.add(ref1);

        final String signedpropsId = "_" + UUID.randomUUID().toString() + signedpropsIdSuffix;

        Reference ref2 = fac.newReference("#" + signedpropsId, fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(
                        fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (XMLStructure) null)),
                "http://uri.etsi.org/01903/v1.3.2#SignedProperties", null);
        refs.add(ref2);

        Reference ref3 = fac.newReference(null, fac.newDigestMethod(DigestMethod.SHA256, null),
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

        // 4. Set up <ds:Object> with <QualifiyingProperties> inside that includes
        // SigningTime
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

        List<DOMStructure> xmlObj = new ArrayList<DOMStructure>();
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

        // 6. sign it!
        XMLSignature signature = fac.newXMLSignature(si, ki, objects, null, null);
        signature.sign(dsc);

        return doc;
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
