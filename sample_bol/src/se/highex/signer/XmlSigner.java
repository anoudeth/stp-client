/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.highex.signer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import se.cma.smallsystems.pki.xml.NoUriDereferencer;
/**
 *
 * @author Oulaiphone_SI
 */
public class XmlSigner {
    public Document sign(Document doc, X509Certificate signerCertificate, PrivateKey privateKey) throws Exception
	{
		final String xadesNS = "http://uri.etsi.org/01903/v1.3.2#";
		final String signedpropsIdSuffix = "-signedprops";

		XMLSignatureFactory fac = null;
		try
		{
			fac = XMLSignatureFactory.getInstance("DOM", "XMLDSig");
		}
		catch (NoSuchProviderException ex)
		{
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

		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String signingTime = df.format(new Date());

		Element STElement = doc.createElementNS(xadesNS, "xades:SigningTime");
		STElement.appendChild(doc.createTextNode(signingTime));
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

	public boolean verify(Document doc, final X509Certificate signerCertificate) throws Exception
	{
		XPath xpath = XPathFactory.newInstance().newXPath();

		String xpathExpression = "//*[local-name()='Signature']";

		NodeList nodes = (NodeList) xpath.evaluate(xpathExpression, doc.getDocumentElement(), XPathConstants.NODESET);

		if (nodes == null || nodes.getLength() == 0)
			throw new Exception("Signature is missing in the document");

		Node nodeSignature = nodes.item(0);

		final KeySelector mockKeySelector = new KeySelector() {

			@Override
			public KeySelectorResult select(
					KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method, XMLCryptoContext context
					) throws KeySelectorException
			{
				return new KeySelectorResult() {
					@Override
					public Key getKey()
					{
						return signerCertificate.getPublicKey();
					}
				};
			}
		};

		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

		// Set up custom URIDereferencer to process Reference without URI.
		// This Reference points to element <Document> of MX message
		final NodeList docNodes = doc.getElementsByTagName("Document");
		final Node docNode = docNodes.item(0);

		ByteArrayOutputStream refOutputStream = new ByteArrayOutputStream();
		Transformer xform = TransformerFactory.newInstance().newTransformer();
		xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		xform.transform(new DOMSource(docNode), new StreamResult(refOutputStream));
		InputStream refInputStream = new ByteArrayInputStream(refOutputStream.toByteArray());

		DOMValidateContext valContext = new DOMValidateContext(mockKeySelector, nodeSignature);
		valContext.setURIDereferencer(new NoUriDereferencer(refInputStream));

		// Java 1.7.0_25+ complicates validation of
		// ds:Object/QualifyingProperties/SignedProperties
		// See details at https://bugs.openjdk.java.net/browse/JDK-8019379
		//
		// One of the solutions is to register the Id attribute using the
		// DOMValidateContext.setIdAttributeNS
		// method before validating the signature
		NodeList nl = doc.getElementsByTagNameNS("http://uri.etsi.org/01903/v1.3.2#", "SignedProperties");
		if (nl.getLength() == 0)
			throw new Exception("SignerProperties is missing in signature");

		Element elemSignedProps = (Element) nl.item(0);
		valContext.setIdAttributeNS(elemSignedProps, null, "Id");

		XMLSignature signature = fac.unmarshalXMLSignature(valContext);

		return signature.validate(valContext);
	}
}
