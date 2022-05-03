package org.example;

import static org.example.utils.CryptoText.*;

import org.apache.commons.codec.binary.Base64;
import org.example.utils.ConsoleColors;
import org.junit.Assert;
import org.junit.Test;

public class AppTest {

    public void testAlgorithm(String algorithm) {
        System.out.println(ConsoleColors.GREEN_BOLD + "Testowanie " + algorithm + ConsoleColors.RESET);
        String testData = testPassGenerator((int) (10 + Math.round(Math.random() * 10)));
        Long encodeBegin = System.nanoTime();
        String encodedData = encode(testData, algorithm);
        Long encodeEnd = System.nanoTime();
        double diffEncode = (double) (encodeEnd - encodeBegin) / 1_000_000;
        assert encodedData != null;
        String encodedDataB64 = new String(Base64.decodeBase64(encodedData.getBytes()));
        Long decodeBegin = System.nanoTime();
        String decodedData = decode(encodedData, algorithm);
        Long decodeEnd = System.nanoTime();
        double diffDecode = (double) (decodeEnd - decodeBegin) / 1_000_000;
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
}
