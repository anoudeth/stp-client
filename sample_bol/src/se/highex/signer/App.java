/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.highex.signer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

/**
 *
 * @author Oulaiphone_SI
 */
public class App {
    private static final String usage1 = "Usage: java -jar xmlsigner.jar [OPTIONS] signLogon messageFile [outputFile]";
	private static final String usage2 = "Usage: java -jar xmlsigner.jar [OPTIONS] signMessage messageFile [outputFile]";

	private App() {}
	
	public static void main(String[] args) throws Exception
	{
		String action = args.length < 1 ? null : args[0];

		if (!"signLogon".equals(action) && !"signMessage".equals(action))
		{
			System.out.println(usage1);
			System.out.println(usage2);
			return;
		}

		String ksType = System.getProperty("ksType", "PKCS12");
		String ksPath = System.getProperty("ksPath");
		String ksPass = System.getProperty("ksPass");
		String keyAlias = System.getProperty("keyAlias");
		String keyPass = System.getProperty("keyPass", ksPass);
		
		KeyStore privateKS;
		
		try (InputStream is = new FileInputStream(new File(ksPath)))
		{
			privateKS = KeyStore.getInstance(ksType);
			privateKS.load(is, ksPass.toCharArray());
		}

		X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(keyAlias);

		PrivateKey privateKey = (PrivateKey) privateKS.getKey(keyAlias, keyPass.toCharArray());
		if (privateKey == null)
			throw new IllegalStateException("Crypto error, failed to load private key " + keyAlias);
		
		if ("signLogon".equals(action))
		{
			String inFile = args.length < 2 ? null : args[1];
			if (inFile == null || (inFile = inFile.trim()).isEmpty())
			{
				System.out.println(usage1);
				return;
			}

			String message = FileUtils.readFileToString(new File(inFile), Charset.forName("UTF-8"));

			System.out.println("Signing logon \"" + message + "\".");

			String signedString = new Crypto().signValue(message.getBytes(Boolean.getBoolean("useUTF16LE") ? "UTF-16LE" : "UTF-8"), certToSign, privateKey);

			String outputFile = args.length < 3 ? null : args[2];
			if (outputFile == null)
			{
				System.out.println(signedString);
			}
			else
			{
				try (FileOutputStream fos = new FileOutputStream(new File(outputFile)))
				{
					fos.write(signedString.getBytes(Charset.forName("UTF-8")));
				}
			}
		}
		else
		{
			String inFile = args.length < 2 ? null : args[1];
			if (inFile == null || (inFile = inFile.trim()).isEmpty())
			{
				System.out.println(usage2);
				return;
			}

			String outFile = args.length < 3 ? null : args[2];
			if (inFile == null || (inFile = inFile.trim()).isEmpty())
			{
				System.out.println(usage2);
				return;
			}

			try
			{
				Document doc = new XmlSigner()
						.sign(
								DocumentBuilderFactory
									.newInstance()
									.newDocumentBuilder()
									.parse(new File(inFile))
								, certToSign
								, privateKey
						);

				TransformerFactory.newInstance().newTransformer().transform(
						new DOMSource(doc), new StreamResult(new File(outFile))
				);
			}
			catch (KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException | UnrecoverableKeyException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
