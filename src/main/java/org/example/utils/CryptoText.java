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
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@RunWith(SerenityRunner.class)
public class CryptoText {

    //    private static SecretKey keyAes = null;
    private static SecretKey key = null;
    public static final String SALT = "12345ABCabc@#$()";
    private static final String PATH = "src/test/resources/";
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(PATH + ".env")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
    private static Cipher cipher;

    private static SecretKey createAESKey(String keyAesString) throws Exception {
        SecureRandom securerandom = new SecureRandom(keyAesString.getBytes());
        KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
        keygenerator.init(256, securerandom);
        return keygenerator.generateKey();
    }

    private static SecretKey createDESKey(String keyDesString) throws Exception {
        DESKeySpec dks = new DESKeySpec(keyDesString.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(dks);
    }

    public static void saveKey(String key, String value) {
        Path p = Paths.get(PATH + ".env");
        String s = System.lineSeparator() + key + value;
        System.out.println(ConsoleColors.YELLOW + "Nie znalazłem klucza " + key + ", generuje nowy i zapisuje do .env (" + PATH + ".env" + ")..." + ConsoleColors.RESET);
        try {
            Files.write(p, s.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println(ConsoleColors.RED_BOLD + "Wystąpił błąd:" + ex.getMessage() + ConsoleColors.RESET);
        }
    }

    /**
     * Sprawdza, czy został pobrany klucz dla algorytmu określonego w <b>algorithm</b>
     * jeżeli nie, pobiera odpowiedni dla algorytmu klucz z <b>.env</b>
     * jeżeli klucza nie ma w <b>.env</b>, tworzy klucza dla odpowiedniego algorytmu na podstawie {@link CryptoText#SALT}
     * i zapisuje w <b>.env</b>
     *
     * @param algorithm metoda kodowania, możliwy do wyboru <b>DES</b> lub <b>AES</b>
     * @throws Exception obsługuje wyjątki:
     *                   - wygenerowany klucz nie spełnia wymagań,
     *                   - algorytm jest niewłaściwy,
     *                   - wyjątki powstałe w {@link CryptoText#createAESKey} oraz w {@link CryptoText#createDESKey}
     */
    private static void init(@NotNull String algorithm) throws Exception {
        assert algorithm.equals("DES") || algorithm.equals("AES");
        if (key == null) {
            String keyString = dotenv.get("KEY_" + algorithm);
            if (keyString == null) {
                if (algorithm.equals("DES")) {
                    DESKeySpec dks = new DESKeySpec(SALT.getBytes());
                    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
                    String keyDesString = DatatypeConverter.printHexBinary(keyFactory.generateSecret(dks).getEncoded());
                    String keyDesStringEncoded = Base64.encodeBase64String(keyDesString.getBytes(StandardCharsets.UTF_8));
                    key = createDESKey(keyDesStringEncoded);
                    saveKey("KEY_DES=", keyDesStringEncoded);
                    System.out.println("key DES: " + DatatypeConverter.printHexBinary(key.getEncoded()) + ConsoleColors.RESET);
                } else {
                    SecureRandom securerandom = new SecureRandom(SALT.getBytes());
                    KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
                    keygenerator.init(256, securerandom);
                    String keyAesString = DatatypeConverter.printHexBinary(keygenerator.generateKey().getEncoded());
                    String keyAesStringEncoded = Base64.encodeBase64String(keyAesString.getBytes(StandardCharsets.UTF_8));
                    key = createAESKey(keyAesStringEncoded);
                    saveKey("KEY_AES=", keyAesStringEncoded);
                    System.out.println("key AES: " + DatatypeConverter.printHexBinary(key.getEncoded()) + ConsoleColors.RESET);
                }
            } else {
                if (algorithm.equals("DES")) {
                    key = createDESKey(keyString);
                } else {
                    key = createAESKey(keyString);
                }
            }
        } else {
            System.out.println(ConsoleColors.YELLOW + "Pobrano klucz (" + algorithm + ")..." + ConsoleColors.RESET);
        }
    }

    /**
     * Szyfruje tekst <b>data</b> i zwraca zaszyfrowaną wartość przy pomocy algorytmu DES,
     * przed zaszyfrowaniem sprawdza, czy w pliku <b>.env</b> jest już <i>KEY_DES</i>. Jeżeli nie ma, to za pomocą {@link CryptoText#init}
     * i na podstawie wartości zmiennej {@link CryptoText#SALT} tworzy klucz i zapisuje w <b>.env</b>
     *
     * @param data      tekst, który ma zostać zakodowany
     * @param algorithm metoda kodowania, możliwy do wyboru <b>DES</b> lub <b>AES</b>
     * @return String zwraca zakodowany tekst
     * @see CryptoText#saveKey
     * @see CryptoText#createDESKey
     */
    public static String encode(@NotNull String data, @NotNull String algorithm) {
        try {
            assert algorithm.equals("DES") || algorithm.equals("AES");
            init(algorithm);
            cipher = Cipher.getInstance(algorithm);
            try {
                assert key != null;
                cipher.init(Cipher.ENCRYPT_MODE, key);
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
     * rozkodowuje tekst <b>encodedData</b> przy pomocy algorytmu określonego w <b>algorithm</b>,
     *
     * @param encodedData zakodowany tekst za pomocą algorytmu DES
     * @param algorithm   metoda kodowania, możliwy do wyboru <b>DES</b> lub <b>AES</b>
     * @return String zwraca zdekodowany tekst
     */
    public static String decode(@NotNull String encodedData, @NotNull String algorithm) {
        try {
            assert algorithm.equals("DES") || algorithm.equals("AES");
            init(algorithm);
            cipher = Cipher.getInstance(algorithm);
            assert key != null;
            cipher.init(Cipher.DECRYPT_MODE, key);
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

    public static void SaveKeyPair(KeyPair keyPair) throws IOException {
        PrivateKey privateKey = keyPair.getPrivate();
        System.out.println("Private key: " + DatatypeConverter.printHexBinary(privateKey.getEncoded()));
        PublicKey publicKey = keyPair.getPublic();
        System.out.println("Public key: " + DatatypeConverter.printHexBinary(publicKey.getEncoded()));
        // Store Public Key.
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        FileOutputStream fos = new FileOutputStream(PATH + "/public.key");
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();
        // Store Private Key.
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        fos = new FileOutputStream(PATH + "/private.key");
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    public static PublicKey loadPublicKey() {
        try {
            File filePublicKey = new File(PATH + "/public.key");
            FileInputStream fis = new FileInputStream(PATH + "/public.key");
            byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
            fis.read(encodedPublicKey);
            fis.close();
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    public static KeyPair LoadKeyPair() throws Exception {
        // Read Private Key.
        File filePrivateKey = new File(PATH + "/private.key");
        FileInputStream fis = new FileInputStream(PATH + "/private.key");
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();
        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        return new KeyPair(loadPublicKey(), privateKey);
    }
}

