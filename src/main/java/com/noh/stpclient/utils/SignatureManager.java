package com.noh.stpclient.utils;

import jakarta.xml.bind.DatatypeConverter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Service
@AllArgsConstructor
public class SignatureManager {



    public PublicKey loadPublicKeyFromFile(String publicKeyName) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
//        String keyStr = new String(Files.readAllBytes((new File(this.keyPairPath + "KeyPair/" + publicKeyName)).toPath()), StandardCharsets.UTF_8);
        String keyStr = new String(Files.readAllBytes((new File("key/" + publicKeyName)).toPath()), StandardCharsets.UTF_8);
        keyStr = keyStr.replace("-----BEGIN PUBLIC KEY-----", "").replaceAll("\r", "").replaceAll("\n", "").replace("-----END PUBLIC KEY-----", "");
        byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);
        return this.loadPublicKey(keyEncoded);
    }

    public PublicKey loadPublicKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return key == null ? null : KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
    }

    public PrivateKey loadPrivateKeyFromFile(String privateKeyName) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
//        String keyStr = new String(Files.readAllBytes((new File(this.keyPairPath + "KeyPair/" + privateKeyName)).toPath()), StandardCharsets.UTF_8);
        String keyStr = new String(Files.readAllBytes((new File("key/" + privateKeyName)).toPath()), StandardCharsets.UTF_8);
        keyStr = keyStr.replace("-----BEGIN PRIVATE KEY-----", "").replaceAll("\r", "").replaceAll("\n", "").replace("-----END PRIVATE KEY-----", "");
        byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);
        return this.loadPrivateKey(keyEncoded);
    }

    public PrivateKey loadPrivateKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return key == null ? null : KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key));
    }

    public byte[] rsaSign(byte[] data, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(data);
        return signer.sign();
    }

    public String rsaSignToHex(String data, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] signature = this.rsaSign(data.getBytes(), privateKey);
        return DatatypeConverter.printHexBinary(signature);
    }

    public boolean rsaVerify(byte[] data, byte[] signature, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initVerify(publicKey);
        signer.update(data);
        return signer.verify(signature);
    }

    public boolean rsaVerify(String data, String signatureHex, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return this.rsaVerify(data.getBytes(), DatatypeConverter.parseHexBinary(signatureHex), publicKey);
    }

    public String generateSignature(String data) {
        String signature = null;

        try {
            PrivateKey privateKey = this.loadPrivateKeyFromFile("ib-private.key");
            signature = this.rsaSignToHex(data, privateKey);
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return signature;
    }

    public boolean verifySignature(String data, String signatureHex) {
        boolean status = false;

        try {
            PublicKey publicKey = this.loadPublicKeyFromFile("ib-public.pub");
            status = this.rsaVerify(data, signatureHex, publicKey);
        } catch (Exception var5) {
            var5.printStackTrace();
            status = false;
        }

        return status;
    }

    public boolean verifySignatureWithPublicKey(String data, String signatureHex, String publicKeyName) {
        boolean status = false;

        try {
            PublicKey publicKey = this.loadPublicKeyFromFile(publicKeyName);
            status = this.rsaVerify(data, signatureHex, publicKey);
        } catch (Exception var6) {
            var6.printStackTrace();
            status = false;
        }

        return status;
    }

    public String generateSignatureWithPrivate(String data, String privateKeyName) {
        String signature = null;

        try {
            PrivateKey privateKey = this.loadPrivateKeyFromFile(privateKeyName);
            signature = this.rsaSignToHex(data, privateKey);
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        return signature;
    }
}
