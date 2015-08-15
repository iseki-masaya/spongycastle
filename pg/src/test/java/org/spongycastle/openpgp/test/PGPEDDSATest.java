package org.spongycastle.openpgp.test;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Iterator;

import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECNamedDomainParameters;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBEProtectionRemoverFactory;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.test.SimpleTest;

public class PGPEDDSATest
    extends SimpleTest
{
    public void performTest()
        throws Exception
    {
        System.out.println("import before 1");
        KeyPairGenerator        keyGen = KeyPairGenerator.getInstance("EDDSA", "SC");

        keyGen.initialize(new ECGenParameterSpec("ed25519"));

        KeyPair kpSign = keyGen.generateKeyPair();

        PGPKeyPair ecdsaKeyPair = new JcaPGPKeyPair(PGPPublicKey.EDDSA, kpSign, new Date());

        //
        // try a signature
        //
        PGPSignatureGenerator signGen = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(PGPPublicKey.EDDSA, HashAlgorithmTags.SHA256).setProvider("SC"));

        signGen.init(PGPSignature.BINARY_DOCUMENT, ecdsaKeyPair.getPrivateKey());

        signGen.update("hello world!".getBytes());

        PGPSignature sig = signGen.generate();

        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("SC"), ecdsaKeyPair.getPublicKey());

        sig.update("hello world!".getBytes());

        if (!sig.verify())
        {
            fail("signature failed to verify!");
        }

        System.out.println("import before 2");

        //
        // generate a key ring
        //
        char[] passPhrase = "test".toCharArray();
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, ecdsaKeyPair,
                 "test@bouncycastle.org", sha1Calc, null, null, new JcaPGPContentSignerBuilder(ecdsaKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha1Calc).setProvider("SC").build(passPhrase));

        PGPPublicKeyRing pubRing = keyRingGen.generatePublicKeyRing();

        PGPSecretKeyRing secRing = keyRingGen.generateSecretKeyRing();

        KeyFingerPrintCalculator fingerCalc = new JcaKeyFingerprintCalculator();

        PGPPublicKeyRing pubRingEnc = new PGPPublicKeyRing(pubRing.getEncoded(), fingerCalc);

        if (!Arrays.areEqual(pubRing.getEncoded(), pubRingEnc.getEncoded()))
        {
            fail("public key ring encoding failed");
        }

        System.out.println("import before 3");

        PGPSecretKeyRing secRingEnc = new PGPSecretKeyRing(secRing.getEncoded(), fingerCalc);

        if (!Arrays.areEqual(secRing.getEncoded(), secRingEnc.getEncoded()))
        {
            fail("secret key ring encoding failed");
        }

        System.out.println("import before 4");


        //
        // try a signature using encoded key
        //
        signGen = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(PGPPublicKey.EDDSA, HashAlgorithmTags.SHA256).setProvider("SC"));

        signGen.init(PGPSignature.BINARY_DOCUMENT, secRing.getSecretKey().extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("SC").build(passPhrase)));

        signGen.update("hello world!".getBytes());

        sig = signGen.generate();

        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("SC"), secRing.getSecretKey().getPublicKey());

        sig.update("hello world!".getBytes());

        System.out.println("import before 5");

        if (!sig.verify())
        {
            fail("re-encoded signature failed to verify!");
        }

        importKeyTest();
    }

    private void importKeyTest()
        throws Exception
    {
        System.out.println("import before");

        byte[] testPubKey =
            Base64.decode(
                "mDMEU/NfCxYJKwYBBAHaRw8BAQdAPwmJlL3ZFu1AUxl5NOSofIBzOhKA1i+AEJku" +
                "Q+47JAa0NEVkRFNBIHNhbXBsZSBrZXkgMSAoZHJhZnQta29jaC1lZGRzYS1mb3It" +
                "b3BlbnBncC0wMCmIeQQTFggAIQUCU/NfCwIbAwULCQgHAgYVCAkKCwIEFgIDAQIe" +
                "AQIXgAAKCRCM/eEhl5ZamnNOAP9pKn5wz3jPsgy9p65zxz1+xJEr/cczFQx/tYkk" +
                "49tkeAD+P9jJE4SFD2lVofxn1e22H7YLvcVyHDOA9gpYWTNXiAU=");

        byte[] testPrivKey =
            Base64.decode(
                "lIYEU/NfCxYJKwYBBAHaRw8BAQdAPwmJlL3ZFu1AUxl5NOSofIBzOhKA1i+AEJku" +
                "Q+47JAb+BwMCeZTNZ5R2udDknlhWE5VnJaHe+HFieLlfQA+nibymcJS5lTYL7NP+" +
                "3CY63ylHwHoS7PuPLpdbEvROJ60u6+a/bSe86jRcJODR6rN2iG9v5LQ0RWREU0Eg" +
                "c2FtcGxlIGtleSAxIChkcmFmdC1rb2NoLWVkZHNhLWZvci1vcGVucGdwLTAwKYh5" +
                "BBMWCAAhBQJT818LAhsDBQsJCAcCBhUICQoLAgQWAgMBAh4BAheAAAoJEIz94SGX" +
                "llqac04A/2kqfnDPeM+yDL2nrnPHPX7EkSv9xzMVDH+1iSTj22R4AP4/2MkThIUP" +
                "aVWh/GfV7bYftgu9xXIcM4D2ClhZM1eIBQ==");

        System.out.println("import");

        PGPUtil.setDefaultProvider("SC");

        //
        // Read the public key
        //
        PGPPublicKeyRing        pubKeyRing = new PGPPublicKeyRing(testPubKey, new JcaKeyFingerprintCalculator());

        System.out.println("after make ring");

        for (Iterator it = pubKeyRing.getPublicKey().getSignatures(); it.hasNext();)
        {
            PGPSignature certification = (PGPSignature)it.next();

            certification.init(new JcaPGPContentVerifierBuilderProvider().setProvider("SC"), pubKeyRing.getPublicKey());

            if (!certification.verifyCertification((String)pubKeyRing.getPublicKey().getUserIDs().next(), pubKeyRing.getPublicKey()))
            {
                fail("self certification does not verify");
            }
        }

        //
        // Read the private key
        //
        PGPSecretKeyRing        secretKeyRing = new PGPSecretKeyRing(testPrivKey, new JcaKeyFingerprintCalculator());
    }

    public String getName()
    {
        return "PGPEDDSATest";
    }

    public static void main(
        String[]    args)
    {
        Security.addProvider(new BouncyCastleProvider());

        runTest(new PGPECDSATest());
    }
}
