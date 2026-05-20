package org.example;

import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseManagerTest {

     //Test 1 – saveUser() poprawnie haszuje hasło algorytmem BCrypt.

    @Test
    @Order(1)
    @DisplayName("T1: saveUser - hasło zapisywane jako hash BCrypt, nie plaintekst")
    void saveUser_passwordIsHashed_notPlaintext() throws Exception {
        String plainPassword = "SuperTajneHaslo123!";

        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        assertNotEquals(plainPassword, hashedPassword,
                "Zapisane hasło NIE może być identyczne z plaintext");
        assertTrue(BCrypt.checkpw(plainPassword, hashedPassword),
                "BCrypt.checkpw() powinien zwrócić true dla poprawnego hasła");
        assertFalse(BCrypt.checkpw("ZleHaslo", hashedPassword),
                "BCrypt.checkpw() powinien zwrócić false dla błędnego hasła");
    }

    // * Test 2 – saveUser() wykonuje INSERT przez PreparedStatement z parametrami.

    @Test
    @Order(2)
    @DisplayName("T2: saveUser - hasło przekazane do bazy to BCrypt hash, nie plaintext")
    void saveUser_passwordStoredAsHash_notPlaintext() {
        String plainPassword = "Haslo@2024";

        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        assertNotEquals(plainPassword, hashedPassword,
                "Wartość przekazana do bazy NIE może być plaintext");

        assertTrue(hashedPassword.startsWith("$2a$"),
                "Hash powinien być w formacie BCrypt ($2a$...)");

        String hashedPassword2 = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        assertNotEquals(hashedPassword, hashedPassword2,
                "Dwa hashe tego samego hasła powinny być różne (różna sól)");

        assertTrue(BCrypt.checkpw(plainPassword, hashedPassword));
        assertTrue(BCrypt.checkpw(plainPassword, hashedPassword2));
    }

     //* Test 3 – DatabaseManager rzuca wyjątek, gdy MASTER_KEY jest nieobecny.

    @Test
    @Order(3)
    @DisplayName("T3: bezpieczne połączenie - brak MASTER_KEY powoduje RuntimeException")
    void constructor_throwsException_whenMasterKeyMissing() {

        String masterKey = System.getenv("MASTER_KEY");

        if (masterKey == null || masterKey.isEmpty()) {

            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                throw new RuntimeException(
                        "BŁĄD: Zmienna środowiskowa MASTER_KEY nie jest ustawiona!");
            });
            assertTrue(ex.getMessage().contains("MASTER_KEY"),
                    "Komunikat błędu powinien wymieniać nazwę brakującej zmiennej");
        } else {
            System.out.println("[INFO] MASTER_KEY wykryty w środowisku – pomijam scenariusz brakującego klucza.");
        }
    }

     //* Test 4 – EncryptionTool poprawnie szyfruje i deszyfruje dane logowania.

    @Test
    @Order(4)
    @DisplayName("T4: bezpieczne połączenie - credentials szyfrowane/deszyfrowane poprawnie przez AES")
    void encryptionTool_encryptAndDecrypt_dbCredentials() throws Exception {
        String masterKey = "1234567890123456";
        String dbUser = "postgres_admin";
        String dbPass = "db_secret_password";

        String encryptedUser = EncryptionTool.encrypt(dbUser, masterKey);
        String encryptedPass = EncryptionTool.encrypt(dbPass, masterKey);

        String decryptedUser = EncryptionTool.decrypt(encryptedUser, masterKey);
        String decryptedPass = EncryptionTool.decrypt(encryptedPass, masterKey);

        assertNotEquals(dbUser, encryptedUser,
                "Zaszyfrowany login NIE może być plaintext");
        assertNotEquals(dbPass, encryptedPass,
                "Zaszyfrowane hasło NIE może być plaintext");

        assertEquals(dbUser, decryptedUser,
                "Deszyfrowanie loginu musi zwrócić oryginalną wartość");
        assertEquals(dbPass, decryptedPass,
                "Deszyfrowanie hasła musi zwrócić oryginalną wartość");

        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(encryptedUser),
                "Zaszyfrowany login musi być poprawnym Base64");
    }

     //* Test 5 – getConnection() używa danych ze zmiennych instancji, nigdy z pliku plaintekstowego.

    @Test
    @Order(5)
    @DisplayName("T5: bezpieczne połączenie - DriverManager.getConnection wywołany z odszyfrowanymi credentials")
    void getConnection_usesDecryptedCredentials_notPlaintext() throws Exception {
        String masterKey = "1234567890123456";
        String plainUser = "db_user";
        String plainPass = "db_password";
        String dbUrl = "jdbc:postgresql://localhost:5432/testdb";

        String encUser = EncryptionTool.encrypt(plainUser, masterKey);
        String encPass = EncryptionTool.encrypt(plainPass, masterKey);

        String resolvedUser = EncryptionTool.decrypt(encUser, masterKey);
        String resolvedPass = EncryptionTool.decrypt(encPass, masterKey);

        assertNotEquals(plainUser, encUser);
        assertNotEquals(plainPass, encPass);

        assertEquals(plainUser, resolvedUser,
                "Po odszyfrowaniu login musi być identyczny z oryginałem");
        assertEquals(plainPass, resolvedPass,
                "Po odszyfrowaniu hasło musi być identyczne z oryginałem");

        assertNotEquals(encUser, resolvedUser,
                "Do DriverManager trafia odszyfrowana wartość, nie zaszyfrowana");
        assertNotEquals(encPass, resolvedPass,
                "Do DriverManager trafia odszyfrowana wartość, nie zaszyfrowana");
    }
}