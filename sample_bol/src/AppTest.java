package se.highex.signer;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Oulaiphone_SI
 */
public class AppTest {
     @Test
    public void testXmlSignature() throws Exception
    {
		KeyStore privateKS;

		char[] pwd = "P@ssw0rd".toCharArray();
		String alias = "mykey";
		
		try (InputStream is = new FileInputStream(new File("src/test/resources/test.jks")))
		{
			privateKS = KeyStore.getInstance("JKS");
			privateKS.load(is, pwd);
		}

		X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(alias);
		PrivateKey privateKey = (PrivateKey) privateKS.getKey(alias, pwd);

		XmlSigner xmlSigner = new XmlSigner();

		Document signedDoc = xmlSigner.sign(
    			DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("src/test/resources/test.xml")),
    			certToSign,
    			privateKey
    	);

		assertTrue(xmlSigner.verify(signedDoc, certToSign));
    }
}
