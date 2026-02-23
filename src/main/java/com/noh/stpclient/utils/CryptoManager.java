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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

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
