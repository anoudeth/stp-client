import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
public final class App {

        public static void main(String[] args) throws Exception {
                String ksType = "PKCS12";
                String ksPath = "J:\\My Drive\\LBB\\STPG\\sample_code_bol\\LBBCLALABXXX.pfx";
                String ksPass = "2wsx@WSX";
                String keyAlias = "te-c44b72d1-77d0-4664-bb5c-a61eaa6fe971";
                // String keyPass = "2wsx@WSX";
                File xmlFile = new File("J:\\My Drive\\LBB\\STPG\\soap\\pac008.xml");
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = factory.newDocumentBuilder();
                Document doc = dBuilder.parse(xmlFile);

                KeyStore privateKS;

                try (InputStream is = new FileInputStream(new File(ksPath))) {
                        privateKS = KeyStore.getInstance(ksType);
                        privateKS.load(is, ksPass.toCharArray());
                }

                X509Certificate certToSign = (X509Certificate) privateKS.getCertificate(keyAlias);

                PrivateKey privateKey = (PrivateKey) privateKS.getKey(keyAlias, ksPass.toCharArray());
                if (privateKey == null)
                        throw new IllegalStateException("Crypto error, failed to load private key " + keyAlias);

                toString(new MXdocument().sign(doc, certToSign, privateKey));

        }

        private static void toString(Document newDoc) throws Exception {
                TransformerFactory tranFactory = TransformerFactory.newInstance();
                Transformer aTransformer = tranFactory.newTransformer();
                Source src = new DOMSource(newDoc);
                Result dest = new StreamResult(System.out);
                aTransformer.transform(src, dest);
        }
}