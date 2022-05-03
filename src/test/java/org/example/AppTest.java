package org.example;

import static org.example.utils.CryptoText.*;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.codec.binary.Base64;
import org.example.utils.ConsoleColors;
import org.example.utils.CryptoText;
import org.junit.Assert;
import org.junit.Test;

import java.security.*;
import java.util.concurrent.atomic.AtomicReference;

public class AppTest {
    private static final Integer NANO_TO_MILLISECONDS = 1_000_000;
    private static final String PATH = "src/test/resources/.env";
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(PATH)
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
    static AtomicReference<String> encodedData = new AtomicReference<>("");
    static AtomicReference<String> decodedData = new AtomicReference<>("");

    private static void withTimeMeasurement(String algorithm, String format, String operation, Runnable original) {
        Long timeBegin = System.nanoTime();
        original.run();
        Long timeEnd = System.nanoTime();
        double diff = (double) (timeEnd - timeBegin) / NANO_TO_MILLISECONDS;
        if (operation.equals("encode")) {
            System.out.printf(format, algorithm, encodedData.get(), diff);
        } else {
            System.out.printf(format, algorithm, decodedData.get(), diff);
        }

    }

    private void testAlgorithm(String algorithm) {
        System.out.println(ConsoleColors.GREEN_BOLD + "Testowanie " + algorithm + ConsoleColors.RESET);
        String testData = testPassGenerator((int) (10 + Math.round(Math.random() * 10)));
        System.out.println("Tekst testowy: " + ConsoleColors.WHITE_BOLD_BRIGHT + testData + ConsoleColors.RESET);
        withTimeMeasurement(algorithm, "Tekst zakodowany %s: %s czas kodowania: %s min. sek.%n", "encode", () -> encodedData.set(encode(testData, algorithm)));
        assert encodedData.get() != null;
        String encodedDataB64 = new String(Base64.decodeBase64(encodedData.get().getBytes()));
        System.out.println("Tekst zdekodowany Base64: " + encodedDataB64);
        withTimeMeasurement(algorithm, "Tekst zdekodowany %s: " + ConsoleColors.WHITE_BOLD_BRIGHT + "%s" + ConsoleColors.RESET + " czas dekodowania: %s min. sek.%n", "decode", () -> decodedData.set(decode(encodedData.get(), algorithm)));
        Assert.assertEquals(ConsoleColors.RED_BOLD + "Dane testowe '" + testData + "' i '" + decodedData.get() + "' nie są zgodne" + ConsoleColors.RED_BOLD, testData, decodedData.get());
        Assert.assertNotEquals(ConsoleColors.RED_BOLD + "Dane testowe '" + testData + "' i '" + decodedData.get() + "' są zgodne, a nie powinny" + ConsoleColors.RED_BOLD, decodedData.get(), encodedDataB64);
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
    public void generateKeyPair() {
        try {
            SecureRandom secureRandom1 = new SecureRandom(SALT.getBytes());
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            Signature signature = Signature.getInstance("SHA256WithDSA");
            signature.initSign(keyPair.getPrivate(), secureRandom1);
//            byte[] data = "abcdefghijklmnopqrstuvxyz".getBytes(StandardCharsets.UTF_8);
//            signature.update(data);
            byte[] digitalSignature = signature.sign();
            System.out.println("digitalSignature: " + Base64.encodeBase64String(digitalSignature));
            saveKey("KEY_DSA=", Base64.encodeBase64String(digitalSignature));
            SaveKeyPair(keyPair);
        } catch (Exception ex) {
            System.out.println("Jakiś problem z wygenerowaniem kluczy: " + ex.getMessage());
        }
    }

    @Test
    public void verifySignature() {
        try {
            byte[] keyString2 = Base64.decodeBase64(dotenv.get("KEY_DSA"));
            System.out.println("digitalSignature2: " + Base64.encodeBase64String(keyString2));
            Signature signature2 = Signature.getInstance("SHA256WithDSA");
            signature2.initVerify(CryptoText.loadPublicKey());
//            byte[] data = "abcdefghijklmnopqrstuvxyz".getBytes(StandardCharsets.UTF_8);
//            signature2.update(data);
            boolean verified = signature2.verify(keyString2);
            if (verified) {
                System.out.print(ConsoleColors.GREEN_BRIGHT);
            } else {
                System.out.print(ConsoleColors.RED_BRIGHT);
            }
            System.out.println("verified = " + verified + ConsoleColors.RESET);
            assert verified;
        } catch (Exception ex) {
            System.out.println("Coś poszło nie tak przy odbiorze: " + ex.getMessage());
        }
    }

    @Test
    public void all() {
        generateKeyPair();
        verifySignature();
    }


}
