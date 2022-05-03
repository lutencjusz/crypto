package org.example;

import static org.example.utils.CryptoText.*;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.codec.binary.Base64;
import org.example.utils.ConsoleColors;
import org.example.utils.CryptoText;
import org.junit.Assert;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;

public class AppTest {
    private static final Integer NANO_TO_MILLISECONDS = 1_000_000;
    public void testAlgorithm(String algorithm) {
        System.out.println(ConsoleColors.GREEN_BOLD + "Testowanie " + algorithm + ConsoleColors.RESET);
        String testData = testPassGenerator((int) (10 + Math.round(Math.random() * 10)));
        Long encodeBegin = System.nanoTime();
        String encodedData = encode(testData, algorithm);
        Long encodeEnd = System.nanoTime();
        double diffEncode = (double) (encodeEnd - encodeBegin) / NANO_TO_MILLISECONDS;
        assert encodedData != null;
        String encodedDataB64 = new String(Base64.decodeBase64(encodedData.getBytes()));
        Long decodeBegin = System.nanoTime();
        String decodedData = decode(encodedData, algorithm);
        Long decodeEnd = System.nanoTime();
        double diffDecode = (double) (decodeEnd - decodeBegin) / NANO_TO_MILLISECONDS;
        System.out.println("Tekst testowy: " + ConsoleColors.WHITE_BOLD_BRIGHT + testData + ConsoleColors.RESET);
        System.out.println("Tekst zakodowany " + algorithm + ": " + encodedData + ", czas kodowania: " + diffEncode + " min. sek.");
        System.out.println("Tekst zdekodowany Base64: " + encodedDataB64);
        System.out.println("Tekst zdekodowany " + algorithm + ": " + ConsoleColors.WHITE_BOLD_BRIGHT + decodedData + ConsoleColors.RESET + " czas dekodowania: " + diffDecode + " min. sek.");
        Assert.assertEquals(ConsoleColors.RED_BOLD + "Dane testowe '" + testData + "' i '" + decodedData + "' nie są zgodne" + ConsoleColors.RED_BOLD, testData, decodedData);
        Assert.assertNotEquals(ConsoleColors.RED_BOLD + "Dane testowe '" + testData + "' i '" + decodedData + "' są zgodne, a nie powinny" + ConsoleColors.RED_BOLD, decodedData, encodedDataB64);
    }

    @Test
    public void testDES() {
        testAlgorithm("DES");
    }

    @Test
    public void testAES() {
        testAlgorithm("AES");
    }

    @Test
    public void generate_keyPairs(){
        final String PATH = "src/test/resources/.env";
        final Dotenv dotenv = Dotenv.configure()
                .directory(PATH)
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        final String SALT = "12345ABCabc@#$()";
        try {
            SecureRandom secureRandom1 = new SecureRandom(SALT.getBytes());
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            Signature signature = Signature.getInstance("SHA256WithDSA");
            signature.initSign(keyPair.getPrivate(), secureRandom1);
            byte[] data = "abcdefghijklmnopqrstuvxyz".getBytes("UTF-8");
//            signature.update(data);
            byte[] digitalSignature = signature.sign();
            System.out.println("digitalSignature: "+ new String(digitalSignature));
            CryptoText.saveKey("KEY_DSA=", new String(digitalSignature));


            String keyString2 = dotenv.get("KEY_DSA");
            Signature signature2 = Signature.getInstance("SHA256WithDSA");
            signature2.initVerify(keyPair.getPublic());
//            signature2.update(data);
            boolean verified = signature2.verify(keyString2.getBytes());
            System.out.println("verified = " + verified);
        } catch (Exception ex){
            System.out.println("Jakiś problem z wygenerowaniem kluczy");
        }

    }

}
