package org.example.utils;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.xml.bind.DatatypeConverter;
import net.serenitybdd.junit.runners.SerenityRunner;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.passay.CharacterData;
import org.passay.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RunWith(SerenityRunner.class)
public class CryptoText {

    //    private static SecretKey keyAes = null;
    private static SecretKey keyDes = null;
    private static final String SOIL = "12345ABCabc@#$()";
    private static final String pathStr = "src/test/resources/.env";
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(pathStr)
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
    private static Cipher cipher;

//    private static SecretKey createAESKey() throws Exception {
//        SecureRandom securerandom = new SecureRandom(SOIL.getBytes());
//        KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
//        keygenerator.init(256, securerandom);
//        return keygenerator.generateKey();
//    }

    private static SecretKey createDESKey(String keyDesString) throws Exception {
        DESKeySpec dks = new DESKeySpec(keyDesString.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(dks);
    }

    private static void saveKey(String key, String value) {
        Path p = Paths.get(pathStr);
        String s = System.lineSeparator() + key + value;
        System.out.println(ConsoleColors.YELLOW + "Nie znalazłem klucza " + key + ", generuje nowy i zapisuje do .env (" + pathStr + ")...");
        try {
            Files.write(p, s.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println(ConsoleColors.RED_BOLD + "Wystąpił błąd:" + ex.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void init() throws Exception {
        if (keyDes == null) {
            String keyDesString = dotenv.get("KEY_DES");
            if (keyDesString == null) {
                DESKeySpec dks = new DESKeySpec(SOIL.getBytes());
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
                keyDesString = DatatypeConverter.printHexBinary(keyFactory.generateSecret(dks).getEncoded());
                String keyDesStringEncoded = Base64.encodeBase64String(keyDesString.getBytes(StandardCharsets.UTF_8));
                keyDes = createDESKey(keyDesStringEncoded);
                saveKey("KEY_DES=", keyDesStringEncoded);
                System.out.println("key: " + DatatypeConverter.printHexBinary(keyDes.getEncoded()) + ConsoleColors.RESET);
            } else {
                keyDes = createDESKey(keyDesString);
            }
        }
    }

    /**
     * koduje tekst <b>data</b> i zwraca zakodowaną wartość przy pomocy algorytmu DES,
     * jeżeli w pliku <b>.env</b> nie znalazł klucza <i>KEY_DES</i> to za pomocą {@link CryptoText#init()}
     * klucz zostanie utworzony i zapisany w <b>.env</b>
     *
     * @param data tekst, który ma zostać zakodowany
     * @return String zwraca zakodowany tekst
     * @see CryptoText#saveKey
     * @see CryptoText#createDESKey
     */
    public static String encodeDES(@NotNull String data) {
        try {
            init();
            cipher = Cipher.getInstance("DES");
            try {
                assert keyDes != null;
                cipher.init(Cipher.ENCRYPT_MODE, keyDes);
            } catch (NullPointerException ex) {
                System.out.println(ConsoleColors.RED_BOLD + "Klucz jest pusty" + ConsoleColors.RESET);
            }
            return Base64.encodeBase64String(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            System.out.println(ConsoleColors.RED_BOLD + "Nastąpił błąd: " + ex.getMessage() + ConsoleColors.RESET);
            return null;
        }
    }

    /**
     * rozkodowuje tekst <b>encodedData</b> przy pomocy algorytmu DES,
     *
     * @param encodedData zakodowany tekst za pomocą algorytmu DES
     * @return String zwraca zdekodowany tekst
     */
    public static String decodeDES(@NotNull String encodedData) {
        try {
            init();
            cipher = Cipher.getInstance("DES");
            assert keyDes != null;
            cipher.init(Cipher.DECRYPT_MODE, keyDes);
            return new String(cipher.doFinal(Base64.decodeBase64(encodedData.getBytes())));
        } catch (Exception ex) {
            System.out.println(ConsoleColors.RED_BOLD + "Nastąpił błąd: " + ex.getMessage() + ConsoleColors.RESET);
            return null;
        }
    }

    public static String testPassGenerator(int passLength) {

        PasswordGenerator gen = new PasswordGenerator();

        CharacterData specialChars = new CharacterData() {
            public String getErrorCode() {
                return ConsoleColors.RED_BOLD + "Nie udało się wygenerować znaków specjalnych" + ConsoleColors.RESET;
            }

            public String getCharacters() {
                return "!@#$%^&*()_+?><";
            }
        };

        CharacterData lowerCaseChars = EnglishCharacterData.LowerCase;
        CharacterRule lowerCaseRule = new CharacterRule(lowerCaseChars);
        lowerCaseRule.setNumberOfCharacters(Math.floorDiv(passLength, 2));
        CharacterData upperCaseChars = PolishCharacterData.UpperCase;
        CharacterRule upperCaseRule = new CharacterRule(upperCaseChars);
        upperCaseRule.setNumberOfCharacters(Math.floorDiv(passLength, 10));
        CharacterData digitChars = EnglishCharacterData.Digit;
        CharacterRule digitRule = new CharacterRule(digitChars);
        digitRule.setNumberOfCharacters(Math.floorDiv(passLength, 10));
        CharacterRule splCharRule = new CharacterRule(specialChars);
        splCharRule.setNumberOfCharacters(Math.floorDiv(passLength, 10));

        return gen.generatePassword(passLength, splCharRule, lowerCaseRule, upperCaseRule, digitRule);
    }
}

