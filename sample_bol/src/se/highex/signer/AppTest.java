/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.highex.signer;

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
		
		try (InputStream is = new FileInputStream(new File("D:\\oulaiphone file\\Support Division\\ATS\\UAT\\SHB\\ca\\SHBALALABXXX.jks")))
		{
			privateKS = KeyStore.getInstance("JKS");
			privateKS.load(is, pwd);
		}

		X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(alias);
		PrivateKey privateKey = (PrivateKey) privateKS.getKey(alias, pwd);

		XmlSigner xmlSigner = new XmlSigner();

		Document signedDoc = xmlSigner.sign(
    			DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("D:\\oulaiphone file\\Support Division\\ATS\\UAT\\SHB\\rtgs shb prod.xml")),
    			certToSign,
    			privateKey
    	);

		assertTrue(xmlSigner.verify(signedDoc, certToSign));
    }
}
