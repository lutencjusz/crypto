package org.example;

import static org.example.utils.CryptoText.*;

import org.apache.commons.codec.binary.Base64;
import org.example.utils.ConsoleColors;
import org.junit.Assert;
import org.junit.Test;

public class AppTest {

    @Test
    public void test() {
        try {
            System.out.println(ConsoleColors.GREEN_BOLD + "Testowanie DES" + ConsoleColors.RESET);
            String testData = testPassGenerator((int) (10 + Math.round(Math.random() * 10)));
            String encodedData = encodeDES(testData);
            assert encodedData != null;
            String encodedDataB64 = new String(Base64.decodeBase64(encodedData.getBytes()));
            String decodedData = decodeDES(encodedData);
            System.out.println("Tekst testowy: " + ConsoleColors.WHITE_BOLD_BRIGHT + testData + ConsoleColors.RESET);
            System.out.println("Tekst zakodowany: " + encodedData);
            System.out.println("Tekst zdekodowany Base64: " + encodedDataB64);
            System.out.println("Tekst zdekodowany: " + ConsoleColors.WHITE_BOLD_BRIGHT + decodedData + ConsoleColors.RESET);
            Assert.assertEquals(ConsoleColors.RED_BOLD + "Dane testowe '" + testData + "' i '" + decodedData + "' nie są zgodne" + ConsoleColors.RED_BOLD, testData, decodedData);
            Assert.assertNotEquals(ConsoleColors.RED_BOLD + "Dane testowe '" + testData + "' i '" + decodedData + "' są zgodne, a nie powinny" + ConsoleColors.RED_BOLD, decodedData, encodedDataB64);
        } catch (Exception ex) {
            System.err.println(ConsoleColors.RED_BOLD + "Wystąpił błąd:" + ex.getMessage() + ConsoleColors.RESET);
        }
    }
}
