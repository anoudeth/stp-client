/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author nOi
 */
public class App {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, UnrecoverableKeyException {
      String ksType = "PKCS12";
		String ksPath = "D:\\\\TouFiles\\\\LaPASS UAT\\\\LApnet\\\\LPDRLALALPMS.pfx";
		String ksPass = "2wsx@WSX";
		String keyAlias = "te-abbb59e1-6edd-42c8-965b-45946cc676bf";
		String keyPass = "2wsx@WSX";
		
		KeyStore privateKS;
		
		try (InputStream is = new FileInputStream(ksPath))
		{
			privateKS = KeyStore.getInstance(ksType);
			privateKS.load(is, ksPass.toCharArray());
		}

		X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(keyAlias);

		PrivateKey privateKey = (PrivateKey) privateKS.getKey(keyAlias, keyPass.toCharArray());
		if (privateKey == null)
			throw new IllegalStateException("Crypto error, failed to load private key " + keyAlias);
		
		System.out.println(new Crypto().signValue("1", certToSign, privateKey));
    }
    
}



