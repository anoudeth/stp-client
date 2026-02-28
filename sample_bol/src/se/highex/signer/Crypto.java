/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.highex.signer;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
/**
 *
 * @author Oulaiphone_SI
 */
public class Crypto {
    private final JcaSignerInfoGeneratorBuilder builder;
	private final JcaContentSignerBuilder jcaContentSignerBuilder;

	private static final String PROVIDER = "BC";
	private static final String ALGORITHM = "SHA256withRSA";
	
	public Crypto()
	{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		try
		{
			builder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(PROVIDER).build());
		}
		catch (OperatorCreationException e)
		{
			throw new RuntimeException(e);
		}

		jcaContentSignerBuilder = new JcaContentSignerBuilder(ALGORITHM).setProvider(PROVIDER);
	}

	public String signValue(byte[] buf, X509Certificate signerCertificate, PrivateKey privateKey)
	{
		try
		{
			CMSTypedData typedData = new CMSProcessableByteArray(buf);
			CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

			ContentSigner signer = jcaContentSignerBuilder.build(privateKey);

			gen.addSignerInfoGenerator(builder.build(signer, signerCertificate));
			CMSSignedData signed = gen.generate(typedData, false);
			byte[] der = signed.getEncoded();

			return Base64.getEncoder().encodeToString(der);
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
