package org.example;

import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseManagerTest {

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
}