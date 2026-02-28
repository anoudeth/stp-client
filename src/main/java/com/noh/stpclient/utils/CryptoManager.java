package com.noh.stpclient.utils;

import lombok.AllArgsConstructor;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
        org.w3c.dom.Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));

        KeyStore privateKS = loadKeystore();
        X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey) privateKS.getKey(keyAlias, this.ksPass.toCharArray());
        if (privateKey == null) {
            throw new IllegalStateException("Crypto error, failed to load private key " + keyAlias);
        }

        org.w3c.dom.Document signedDoc = signDocument(doc, certToSign, privateKey);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer().transform(
                new DOMSource(signedDoc), new StreamResult(out));
        return out.toString(StandardCharsets.UTF_8);
    }

    private org.w3c.dom.Document signDocument(org.w3c.dom.Document doc,
                                               X509Certificate signerCertificate,
                                               PrivateKey privateKey) throws Exception {
        final String xadesNS = "http://uri.etsi.org/01903/v1.3.2#";
        final String signedpropsIdSuffix = "-signedprops";

        XMLSignatureFactory fac;
        try {
            fac = XMLSignatureFactory.getInstance("DOM", "XMLDSig");
        } catch (NoSuchProviderException ex) {
            fac = XMLSignatureFactory.getInstance("DOM");
        }

        // 1. Prepare KeyInfo
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509IssuerSerial x509is = kif.newX509IssuerSerial(
                signerCertificate.getIssuerX500Principal().toString(),
                signerCertificate.getSerialNumber());
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
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                refs);

        // 3. Create element AppHdr/Sgntr that will contain the <ds:Signature>
        NodeList appHdrNodes = doc.getElementsByTagNameNS("*", "AppHdr");
        if (appHdrNodes.getLength() == 0) {
            throw new IllegalStateException("Mandatory element AppHdr is missing in the document to be signed");
        }
        Node appHdrNode = appHdrNodes.item(0);
        Node sgntr = appHdrNode.appendChild(doc.createElementNS(appHdrNode.getNamespaceURI(), "Sgntr"));

        DOMSignContext dsc = new DOMSignContext(privateKey, sgntr);
        dsc.putNamespacePrefix(XMLSignature.XMLNS, "ds");

        // 4. Set up <ds:Object> with <QualifyingProperties> inside that includes SigningTime
        Element qpElement = doc.createElementNS(xadesNS, "xades:QualifyingProperties");
        qpElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", xadesNS);

        Element spElement = doc.createElementNS(xadesNS, "xades:SignedProperties");
        spElement.setAttributeNS(null, "Id", signedpropsId);
        dsc.setIdAttributeNS(spElement, null, "Id");
        spElement.setIdAttributeNS(null, "Id", true);
        qpElement.appendChild(spElement);

        Element sspElement = doc.createElementNS(xadesNS, "xades:SignedSignatureProperties");
        spElement.appendChild(sspElement);

        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Element stElement = doc.createElementNS(xadesNS, "xades:SigningTime");
        stElement.appendChild(doc.createTextNode(df.format(new Date())));
        sspElement.appendChild(stElement);

        XMLObject object = fac.newXMLObject(
                Collections.singletonList(new DOMStructure(qpElement)), null, null, null);

        // 5. Set up custom URIDereferencer to process Reference without URI.
        // This Reference points to element <Document> of MX message.
        NodeList docNodes = doc.getElementsByTagNameNS("*", "Document");
        Node docNode = docNodes.item(0);

        ByteArrayOutputStream refOut = new ByteArrayOutputStream();
        Transformer xform = TransformerFactory.newInstance().newTransformer();
        xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xform.transform(new DOMSource(docNode), new StreamResult(refOut));
        InputStream refInputStream = new ByteArrayInputStream(refOut.toByteArray());
        dsc.setURIDereferencer(new NoUriDereferencer(refInputStream));

        // 6. Sign it!
        XMLSignature signature = fac.newXMLSignature(si, ki, Collections.singletonList(object), null, null);
        signature.sign(dsc);

        return doc;
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
