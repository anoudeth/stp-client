package com.noh.stpclient.utils;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
@Component
public class SignatureManager {

    @Value("${spring.profiles.active}")
    private String active_profile;
    @Value("${signature-bypass}")
    private boolean signature_bypass;

    public SignatureManager() {

    }

    private PublicKey loadPublicKeyFromFile(String clientId) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        log.info("load publicKey from dir: KeyPair" + File.separator + "public_" + active_profile + "_" + clientId + ".key");

//        PemFile pemFile = new PemFile("KeyPair" + File.separator + "public_" + active_profile + "_" + clientId + ".key");
//        System.out.println("PemFile object: " + pemFile.getPemObject());
//        System.out.println("PemFile object.content: " + pemFile.getPemObject().getContent());
//        byte[] keyEncoded = pemFile.getPemObject().getContent();

//        return this.loadPublicKey(keyEncoded);

        String keyStr = new String(Files.readAllBytes((new File("KeyPair" + File.separator + "public_" + active_profile + "_" + clientId + ".key")).toPath()), StandardCharsets.UTF_8);

        keyStr = keyStr.replace("-----BEGIN PUBLIC KEY-----", "").replaceAll("\r", "").replaceAll("\n", "").replace("-----END PUBLIC KEY-----", "");
        byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);


        return keyEncoded == null ? null : KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyEncoded));

    }

    private PublicKey loadPublicKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return key == null ? null : KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
    }

    private PrivateKey loadPrivateKeyFromFile(String keyStr) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
//        log.info("load privateKey from: KeyPair" + File.separator + "private_" + active_profile + "_" + clientId + ".key");
//        String keyStr = new String(Files.readAllBytes((new File("KeyPair/" + File.separator + "private_" + active_profile + "_" + clientId + ".key")).toPath()), StandardCharsets.UTF_8);
//        keyStr = keyStr.replace("-----BEGIN PRIVATE KEY-----", "")
//                .replaceAll("\r", "")
//                .replaceAll("\n", "")
//                .replace("-----END PRIVATE KEY-----", "");
//        System.out.println("private key String: " + keyStr);

        byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);
//        System.out.println("keyEndCode: " + keyEncoded);

//       keyEncoded PemFile pemFile pemFile= new PemFile("KeyPair" + File.separator + "private_" + active_profile + "_" + clientId + ".key");
//        System.out.println("PemFile object: " + pemFile.getPemObject());
//        System.out.println("PemFile object.content: " + pemFile.getPemObject().getContent());
//        byte[] keyEncoded = pemFile.getPemObject().getContent();


        return this.loadPrivateKey(keyEncoded);
    }

    private PrivateKey loadPrivateKey(byte[] key) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        return key == null ? null : KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key));
    }

    private String encryptPub(String data, PublicKey publicKey) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        System.out.println("new sign");
        //--- gen message hash
//        MessageDigest md = MessageDigest.getInstance("SHA-256");
//        byte[] messageHash = md.digest(data.getBytes());

        // encrypt generated hash
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] digitalSignature = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

//        return Base64.encodeBase64String(digitalSignature);
        return DatatypeConverter.printHexBinary(digitalSignature);
    }

    private String encryptPri(String data, PrivateKey privateKey) throws Exception {
        System.out.println("new sign");
        //--- gen message hash
//        MessageDigest md = MessageDigest.getInstance("SHA-256");
//        byte[] messageHash = md.digest(data.getBytes());

        // encrypt generated hash
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        byte[] digitalSignature = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

//        return Base64.encodeBase64String(digitalSignature);
        return DatatypeConverter.printHexBinary(digitalSignature);
    }

    private String decryptPri(String signature, PrivateKey privateKey) throws Exception {

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
//            byte[] decryptedSignature = cipher.doFinal(Base64.decodeBase64(signature));
            byte[] decryptedSignature = cipher.doFinal(DatatypeConverter.parseHexBinary(signature));

            return new String(decryptedSignature);
        } catch (Exception ex) {
            System.out.println("signature invalid");
            ex.printStackTrace();
            return null;
        }
    }

    private String decryptPub(String signature, PublicKey publicKey) throws Exception {

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
//            byte[] decryptedSignature = cipher.doFinal(Base64.decodeBase64(signature));
            byte[] decryptedSignature = cipher.doFinal(DatatypeConverter.parseHexBinary(signature));

            return new String(decryptedSignature);
        } catch (Exception ex) {
            System.out.println("signature invalid");
            ex.printStackTrace();
            return null;
        }
    }


//    private String rsaSignToHex(String data, PrivateKey privateKey) throws Exception {
//        byte[] signature = this.rsaSign(data.getBytes(), privateKey);
//        return DatatypeConverter.printHexBinary(signature);
//
//    }
//    private byte[] rsaSign(byte[] data, byte[] signature, PrivateKey privateKey) throws Exception {
//        Signature signer = Signature.getInstance("SHA256withRSA");
//        signer.initSign(privateKey);
//        signer.update(data);
//        return signer.sign();
//    }

//    public boolean rsaVerify(byte[] data, byte[] signature, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
//        Signature signer = Signature.getInstance("SHA256withRSA");
//        System.out.println("signer: " + signer);
//        signer.initVerify(publicKey);
//        signer.update(data);
//        return signer.verify(signature);
//    }
//
//    public boolean rsaVerify(String data, String signatureHex, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
//        return this.rsaVerify(data.getBytes(), DatatypeConverter.parseHexBinary(signatureHex), publicKey);
//    }

//    public boolean rsaVerifyPub(String data, String signatureHex, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
//        Signature signer = Signature.getInstance("SHA256withRSA");
//        signer.initVerify(publicKey);
//        signer.update(data.getBytes());
//        return signer.verify(DatatypeConverter.parseHexBinary(signatureHex));
//    }
//
//    public String genSignatureWithPublic(String data, String publicKeyName) {
//        String signature = null;
//
//        try {
//            PublicKey publicKey = this.loadPublicKeyFromFile(publicKeyName);
//            System.out.println("publicKey: " + publicKey);
//            signature = this.encryptPub(data, publicKey);
////            signature = this.sign(data, privateKey);
//        } catch (Exception var5) {
//
//            var5.printStackTrace();
//        }
//
//        return signature;
//    }

    public String signWithPrivate(String data, String keyStr) {
        try {
            // string replace " to '
//            data = data.replace("\"", "'");     // some test for LaoOdoo
            byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);
            PrivateKey priKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyEncoded));
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(priKey);
            signer.update(data.getBytes());
            byte[] signature = signer.sign();
            return DatatypeConverter.printHexBinary(signature);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public String signWithPrivate2(byte[] data, String keyStr) {
        try {
            byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);
            PrivateKey priKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyEncoded));
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(priKey);
            signer.update(data);
            byte[] signature = signer.sign();
            return DatatypeConverter.printHexBinary(signature);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
//    public String genSignatureWithPrivate(String data, String keyStr) {
//        String signature = null;
//        try {
//            KeyFactory kf = KeyFactory.getInstance("RSA");
//            byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);
//            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyEncoded));
//            signature = this.rsaSignPri(data, privateKey);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return signature;
//    }
//    private String rsaSignPri(String data, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
//        Signature signer = Signature.getInstance("SHA256withRSA");
//        signer.initSign(privateKey);
//        signer.update(data.getBytes());
//        byte[] signature = signer.sign();
//        return DatatypeConverter.printHexBinary(signature);
//    }

//    public boolean verifySignatureWithPrivate(String data, String signature, String privateKeyName) throws Exception {
//        PrivateKey privateKey = this.loadPrivateKeyFromFile(privateKeyName);
//
//        String decryptedText = this.decryptPri(signature, privateKey);
//        try {
//            System.out.println("decrypted: " + decryptedText);
//            System.out.println("data: " + data);
//            if (decryptedText.equals(data)) return true;
//        } catch (Exception ex) {
//            System.out.println("signature invalid");
//            ex.printStackTrace();
//            return false;
//        }
//        return false;
//    }


//    public boolean verifySignatureWithPublic(String data, String signature, String clientId) throws Exception {
////        PublicKey publicKey = this.loadPublicKeyFromFile(publicKeyName);
//
//        PemFile pemFile = new PemFile("KeyPair" + File.separator + "public_" + active_profile + "_" + clientId + ".key");
//        byte[] keyEncoded = pemFile.getPemObject().getContent();
//
//        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyEncoded));
//
//        String decryptedText = this.decryptPub(signature, publicKey);
//        try {
//            System.out.println("decrypted: " + decryptedText);
//            System.out.println("data: " + data);
//            if (decryptedText.equals(data)) return true;
//        } catch (Exception ex) {
//            System.out.println("signature invalid");
//            ex.printStackTrace();
//            return false;
//        }
//        return false;
//    }

    public boolean verifyWithPublic(String data, String signatureHex, String keyStr) {
        if (signature_bypass) {
            log.warn("#########################################################################");
            log.warn("#########################################################################");
            log.warn("#################### BY PASS SIGNATURE VERIFICATION #####################");
            log.warn("#########################################################################");
            log.warn("#########################################################################");
            return true;
        }
        try {
            byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);
            PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyEncoded));
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initVerify(pubKey);
            signer.update(data.getBytes());
            return signer.verify(DatatypeConverter.parseHexBinary(signatureHex));
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

//    public boolean verifySignaturePub(String data, String signatureHex, String keyStr) {
//        boolean status = false;
//        try {
//            byte[] keyEncoded = DatatypeConverter.parseBase64Binary(keyStr);
//
//            PublicKey publicKey = keyEncoded == null ? null : KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyEncoded));
////            PublicKey publicKey = this.loadPublicKeyFromFile(clientId);
//
////            status = this.rsaVerify(data, signatureHex, publicKey);
//            status = this.rsaVerifyPub(data, signatureHex, publicKey);
//        } catch (Exception var5) {
//            var5.printStackTrace();
//        }
//        return status;
//    }

    public static byte[] serializeObject(Serializable obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    public static void main(String[] args) throws Exception {
        SignatureManager s = new SignatureManager();
        System.out.println("==> sign with private key");

        JsonObject js1 = new JsonObject();
//        js1.addProperty("merchantId", "M2317729700");
//        js1.addProperty("debitAmount", "1");
//        js1.addProperty("merchantRefId", "mer205995995912");
//        js1.addProperty("mobileNo", "2059959959");
//        js1.addProperty("paymentType", "ACCOUNT");
//        js1.addProperty("debitAcctNo", "0200001103636");
        js1.addProperty("merchantId", "M0000000000");
        js1.addProperty("mobileNo", "2059959959");


        System.out.println("js1: " + js1);
        System.out.println("toJson: " + new Gson().toJson(js1));
        DebitReqMod reqx = new Gson().fromJson(js1, DebitReqMod.class);
        System.out.println("reqx: " + reqx);

        DebitReqMod drReq = new DebitReqMod();
        drReq.setDebitAcctNo("0100000411067");
        drReq.setDebitAmount("1000");
        drReq.setMerchantId("M2300000002");
        drReq.setMerchantRefId("INV0001/09/23");
        drReq.setMobileNo("2096494828");
        drReq.setPaymentType(EPaymType.ACCOUNT);

        DebitConfirmMod drConfirm = new DebitConfirmMod();
        drConfirm.setMerchantId("M0000000000");
        drConfirm.setMerchantRefId("23105497014228001826");
        drConfirm.setIbRefId("232543870534031");
        drConfirm.setMobileNo("2059366665");
        drConfirm.setOtp("123456");
        System.out.println("drReq: " + drReq);
        System.out.println("drConfirm: " + drConfirm);

        byte[] dataBytes = serializeObject(drReq);
        byte[] dataBytes2 = serializeObject(reqx);
        System.out.println("dataBytes: " + dataBytes);
        System.out.println("dataBytes2: " + dataBytes2);

        String privKey = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDi6xhY9Bwjg0me\n" +
                "CqntiBgpZwguZ9WKS6HqjKrNNZG5gr07ftB5fg2mTeZeTy9I3phOW3lKouX0iQTq\n" +
                "lP+bFSUYIxXn8oDM1cnhbcIuZdJwDb3vLtQYE29SQVEKqa8vfep9O3Iv2IAKSkGq\n" +
                "0bMG9NgWy2l/8YzdaGt6su+vy1dTYB6i3b4IJxDBICYCyh58TQ2KAgcmLNWG+S+s\n" +
                "FYgtADvxAFXPZYiuyhnPwxMF/CyMMD++XBP0McYaQxGlo2WZrwb5ulP8gQtKVJx1\n" +
                "lZXOR+FfXnxAPpqyfhGYMTeQlrp3yQT+a4hxP6esy1B8EHMaZ33JF7bcbqkL3AZ1\n" +
                "EYYJkeaxAgMBAAECggEBAI7rXWlhTnu7i9ljhAVcK0OgZhG8Sk2RN0jgEg+vhrnd\n" +
                "s0vOooVytCwjck3B99kggbMQgANXOBhdWGBbOeY2WieqqXhuT1sz57P5Ck1oyjLT\n" +
                "JOaJiwIi84FOLDnYB8OUveTkVRX3eSWCAXwSGXzvJ06sDSWNQJiO1Orx2m7RVo3m\n" +
                "ky40f3wnXeIkEjxKJuc2JZOX6dC/8Ef4BWb4KSw5Krk3ihcQZ0wyStl4j6WejmEF\n" +
                "q2E/TUQK5lt3pSzicswOH1Ed7Ii1ns+FHZSEQzzlYeVIdASJb9wGPxc9uS3A/KQV\n" +
                "yO8h4KJYHmwYv97wQeKcrNdjdMJPjqd7GRIpEzVu4jECgYEA/vy7lyxOrUsW4LhV\n" +
                "oive/JNaUssvscciWqciwHcQoe7vHT0M9jruipAF9SlZoyeKSBOpWoJP5XI0MTbA\n" +
                "VRQ6NIDaQrychLT/CPwD8dlRZxfPiN5qAohki2ocQrGSua8LTjQwgRdsOJ2REhfy\n" +
                "n+q1W0NboxeiwogQOphyh/WOCSUCgYEA49HSgfMg5ViwScj5M3aVl8pYT2a+PrXF\n" +
                "fsY8hvjJA1jAQhaGs0hBAWLIoHf7O0Tn+qNYM/gt77mdr6ndN9Bm3Ydi7oh2GiDJ\n" +
                "Z244YGdZQhqXe5jFamc52SdPeEUeP1sOWJtc/HJGlnv46xGyg0fRcilycjbZbV7z\n" +
                "l1XgH6V+r50CgYEAuZyLiT3Nf8P7QVWtsEEzLrSsuTwC2exVC5xCZcvGJbpiAOyh\n" +
                "9NNtNRwl2hJhl6x+snzteF8HfcQmTfTHCKeSvwlU1+OoI8oFJCsfS/ufj+X7Qmx0\n" +
                "yqcyWXHCZKISZmwPVLwU2sOMGaJJKdyY1uPpZCeiGxRnfCfDeIPkSyfqXqkCgYAQ\n" +
                "g/J9xzFYTwCgqLggGfWoRlv2jZ03EJhbo2VZ54ky++kcIWPsdU15Gz8uGuSUnF8w\n" +
                "1Uycn948pbkftfG6jRoX7yul3TCqnjvbiqr9miBnYWQf6qhNGWShMG9baa9Sqng0\n" +
                "xjaMeoBRgnU+HU9Sow809no8e2txuVNxYiFYSfgQJQKBgQCTX7atjFkTjojxuwJc\n" +
                "YyVnht9q3/k0ddzKkK8hucXna1wCOdSHuZnKyA3k7jx4VCKqtOp0IJ1aIlmFzicC\n" +
                "LUav7S6R63RdkRsUlIARLIQuw50LsnkB8P4UUMyi1HGv6aVw+fM8WkNhq3gEF4dP\n" +
                "4LfgKbmqSioGGpUZidEBohCOog==";   // bank key
//        String privKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCxKrH0qt144XYfeitQ5M83hHQuWlbOeRK8a\n" +
//                "GWxoiwIslOeiZbZWjlm7nNDiGQAk3S2qnUAcjrzSnrDwTAGISHbiQ8B59TzuMbpFEbz/yB2odngGe8E3xBfrc\n" +
//                "1bcEQSzPgZ9Hiwpfnp3k9ip/6Dbpu/Xe+ZlrDkwEZUNwgLZtLFp8V2WCUb2TD5B9MWAacIHPzoQVOO3Wy63BG\n" +
//                "A/rKajmUrzb/r1q3dlv93NLOc8IKdlm2OQ3n187vrEyN87MW77E6f//LFbQQvDSmZFILKtfGD7EioS0QZ4m5s\n" +
//                "MrbqSc+dAmrlL3Un2g3QVqzsKb+n1Z3D8sgL22Dzhmo6Z8qlvUzdAgMBAAECggEAHxFaIIogCVUcNjvhwa9GK\n" +
//                "TmoeAJui4mOwAxcnPBP4XyIDlqZYecg/sxc2SBfTPyOsIOmvdvKO6S5b39+sOx5d/qKb66cWNrzwqbEJIB+TF\n" +
//                "9oihRyJjwJroWXZ+wBe/yGm2KUEYIhl6Hc8PN9vcWVOVM7M4dJ4OtYyebWmc0/s9pDdzdh+a81hUn9uDiVRof\n" +
//                "rjpAEYT2TmDekryE5wY6CmaNi5vf4tQ+X2CFZsgRUaBPiydl0hf6GuAZqJzAGiqmo2FMqqIhXsT1BLTs9AHJ2\n" +
//                "X219W9dBULwMc+4ol/zuyS5cN12kd3VJRgvVI+8KmVCux2FfQvGsHcCBmMgrhXYTcQKBgQDkJ4Ch8Fn+A6Akt\n" +
//                "fm/cEhNw4n90G8j6YDrmUD1i7MGJAvxbLZxpf6coOJZH5srxlbls7j0+ixTB4nWz9Hx1SGpWpQ5jg9WiS0+t3\n" +
//                "xpT2xuJnsSKU2dIdtsQ/1FfV/nwciEgnGJ+LASS3XWtTsmvGeMV+PLRDjk03D7R4PCwptHBQKBgQDGyiDfRBt\n" +
//                "pbuI076I013TDq6uE5ShVDTRp0RyDAIck+jEPaQrsxXA0EKU+fgutGra9je46Gxux8JkOLidAlxMYnRAmU+8h\n" +
//                "s3ELKJlO8DvZ6Emwn+wmw1aEKvrE5jPgTW3K8tKBQFgFRK2fGWyY7xlf+ZIemRSiQFjPBv0EXWOl+QKBgB9vq\n" +
//                "fOmMGAlk707LaxJBk1gsfS88XNbSx4rQZ4Tn5krAlJDjmfeXVSrfkSVbEX90B9aQhPPHKhcE4v7movduAOjrW\n" +
//                "S4xDhCMm+/zG6eOOx8dNytwDn0Xk1umMkoWzyNoNlRN9+w2mHK7/OjiQDvWL0npwVs4wH0eE7HLcp6EfcZAoG\n" +
//                "AQ6KeEJjBotvBRzWJQOVVqwWLtAmr8VQu0xn8022ojaI6cv3QY8LBbFWFg3+rRVhjeJcDyO1UPPSZfsOhQ06j\n" +
//                "hqpZxlCkehjti1hi4QLHulpYSCoBVSb9Frbw33FbkSOHCZgYmzVjPVeioxEpa9deENN/Lb3z0UiHbtj32TZVI\n" +
//                "zECgYEA2u0zfFExZnYRdNi/GqdAZ/SzzeofpX+KJLArNm3f4efagV8JVH9ObGcq0E3Q+WzcNzW6Fmg5Glt6nb\n" +
//                "fz/3/lv+Viyr18vXvzahYmm/9sjzlmC/SwQWTqSRcetmEvnrPyBf9mO+p70owQIEWWx6euqqsB4yMl1WAcNeQ\n" +
//                "jSX/d2KQ="; // odoo key

        String plain = "{\"debitAcctNo\":\"2058777885\",\"debitAmount\":\"1000\",\"merchantId\":\"M0000000000\",\"merchantRefId\":\"mer-ref-123457\",\"mobileNo\":\"2058777885\",\"paymentType\":\"WALLET\"}";
        String signature = s.signWithPrivate(plain, privKey);
//        String signature = s.signWithPrivate2(dataBytes, privKey);
        String signaturex = s.signWithPrivate2(dataBytes2, privKey);
        System.out.println("signature: " + signature);
        System.out.println("drReq sig: " + s.signWithPrivate(drConfirm.toString(), privKey));
        System.out.println("signaturex: " + signaturex);
        // 2d815710b by Vongkeo
        // 2D815710B by noh
        // C717F57755EF obj model to byte

        //-------------- verify with public key
        System.out.println("==> verify with public key");
        String pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4usYWPQcI4NJngqp7YgY\n" +
                "KWcILmfVikuh6oyqzTWRuYK9O37QeX4Npk3mXk8vSN6YTlt5SqLl9IkE6pT/mxUl\n" +
                "GCMV5/KAzNXJ4W3CLmXScA297y7UGBNvUkFRCqmvL33qfTtyL9iACkpBqtGzBvTY\n" +
                "Fstpf/GM3WhrerLvr8tXU2Aeot2+CCcQwSAmAsoefE0NigIHJizVhvkvrBWILQA7\n" +
                "8QBVz2WIrsoZz8MTBfwsjDA/vlwT9DHGGkMRpaNlma8G+bpT/IELSlScdZWVzkfh\n" +
                "X158QD6asn4RmDE3kJa6d8kE/muIcT+nrMtQfBBzGmd9yRe23G6pC9wGdRGGCZHm\n" +
                "sQIDAQAB"; // bank key
//        String pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1gsAYWJ1StRpeNtkwn1qQJTrjTN6UB5D8i4AXLvnD\n" +
//                "7L3oqwjLAYnX4turjrfQVfrXHYrF8SvyJ8XGpsk1+CunvLNWqW4D/nBjBKhtq9ceQwnXOLpktI2YcOe3XYX/B\n" +
//                "LOaLOSd/MXJc47qjrgSLPv9IvrKQdfRK1DrFBVAfb1Q1dtliJwDxE1AFssdLrZnsYvI/km96/idggkBwFRCHJ\n" +
//                "F+nBifiVV5cgTHcwWtW3OXSSl2Q6WE6Xbg6wjTB1V4xaS/hiCrOvO3UAhNTRgwBh/iM46wcCY+/w0EJrNsU0N\n" +
//                "IVtnrNN5nYvFELb57pK/GWBCpXk7WPC3G50mnX3L16JDUwIDAQAB"; // odoo key

        String signature2 = "2FD9FDCE8AC64D14BAD4780A88AC51D41C089B6EB7056ECC7C97AF86330BC309E3A79D434CAE398F31041B64361BC2B4545DAAB8B6E49B0938722E1AA42A516A5771B910D2D32DFBD696B1CE32A886EE4B99F9BB603F6C37C59417FE417CDF7512D976A8BB58C3A12244F4733483AA7733B29CB33A02659904236A24D3E3A92F72CBEAD82DD4B2CC99516A5AB2E386A86A2F15E0AC8EE0BCEC48B72A7487260D9A63C1B8DFD0C85D63A813564B4FF3A75080B5C8070A46877F1305562816EAAC3351B213976B104CC3C1C71D1AC5FB7A872BC4765C6A73D24DF1DA5514391B6B2170DBC27CF27AA6F2216134BE9582533C13F722EB7C2E4F3AA6CC6E72F02CE7";
//        String signature2 = "dbd09acdc19604d43577abe0408483330ebef1eb205bcacdf9014a9b2f3b08ebbf0e3eead67d3aee40452ecba35a6bcbde334581ee0f15380054b8223f1eb9290fc7f542f24a6a1ed652b5ec5f1981eda9fbaa3949181e677eefd38057241139f2846744700dc0b2bf675bfad7e302e5e2d0f7d9d67665aa5293ee84f8d7779e6953c650204f4dbe3fbd13c1c625921190e1f2972dd82a0961e4e268c2862bfa7ec9efabc6c44498e3acd61c936e73a4810090773e70e1cb66715d4979c086f920c0a5ee71ee21001f1d731069edf8bd42b5308dfb284cfe5af3dfaedf42f508cce9e4acbc790fa05e126f6aed230b6ebebe4efb56ef1803ba24d4672ed25a8b";
//
//        System.out.println("signature2: " + signature2);

//        DebitReqMod req2 = new DebitReqMod();
//        req2.setPaymentType("WALLET");
//        req2.setDebitAcctNo("2059959959");
//        req2.setDebitAmount("1");
//        req2.setMerchantId("M2317729700");
//        req2.setMerchantRefId("mer205995995912");
//        req2.setMobileNo("2059959959");


        boolean b = s.verifyWithPublic(plain, signature2, pubKey);
//        boolean b = s.verifyWithPublic(req.toString(), signature2, pubKey);

        System.out.println("verify pub: " + b);

//        // windows
//        String signature = "56994F7C1BEDC2DB0AB39D0D2D669EF45B3146CB916559C0E4AF823CB633B01F5D7602C938DDE7831D8261E611F857EF62ACA29AC38F4492AD04432A61D0E0A308DBF98810D14F79FA555C4A11C49F5B5C8BF04876B0DC8316748B86066099C0BB2A5A9B85EC60D1F3382F70E4EE13B7FCA9D0D695834BC663C759FD288B600F9CE11EF8A248B49E07ED6675A09D29916A9E94282BBB208B185AA2D6BA6E3BD92A7879B75638555A8E7287161C52F1585EE0DED7ED743FF0E2BAF8582F7F6B0ADD9D9B5B4EA7A9EC2DF691CE00C8DFD63227A270079CEAE78DCD3FF5B61F6957C5B0081EBFC3B39EF4ECC58AD7655DB6C90E1FE43CD20E4D135A5AEAE3484C91";
//
//        //linux
////        String signature = "J6R3REXMJ2wh+h/ISHvfZaG5a+vH2hWS4MP3xr04GSZIal59semSurSg15sAz4JjajWal/ARsokLhgiXdhgUQaQ9PhMVQVTW6zCHXNFyDdBemHZSYQpy5iyy4oFOCFspYukowkKCmzD5jh/iwHej0trn2hHFjNbbX4tB/MN7NlrME8VzuIGhmCyx5Yzd1FzA3kVRY3E2kyN4xfBnTqQF4Xnl4UugD0+ZPJQAHcQwKLqGGLpDa3cVvcX7/qMOGyEpS5VlgOXfBW+P6QBfKTvdV9ihjAFKeHo2WaCDQVy/sduJobkMmCpsFPrYpORL7qkP1kRPkfxycnkcfHvA6nKOYQ==";
//        boolean b = s.verifySignatureWithPrivate(acctInfoRequest.toString(), signature, "TEST");
//        System.out.println("verify: " + b);
    }
}
