package org.example;


import org.mindrot.jbcrypt.BCrypt;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

public class DatabaseManager {
    private String url;
    private String user;
    private String password;

    public DatabaseManager() throws Exception {
        Properties props = new Properties();
        InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties");
        if (is == null) {
            throw new RuntimeException("Nie znaleziono pliku application.properties!");
        }
        props.load(is);

        String masterKey = System.getenv("MASTER_KEY");
        if (masterKey == null || masterKey.isEmpty()) {
            throw new RuntimeException("BŁĄD: Zmienna środowiskowa MASTER_KEY nie jest ustawiona!");
        }

        this.url = props.getProperty("db.url");
        String encryptedUser = props.getProperty("db.user.encrypted");
        String encryptedPass = props.getProperty("db.pass.encrypted");

        this.user = EncryptionTool.decrypt(encryptedUser, masterKey);
        this.password = EncryptionTool.decrypt(encryptedPass, masterKey);

        System.out.println(">>> Konfiguracja bazy załadowana pomyślnie.");
    }

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(this.url, this.user, this.password);
    }

    public void setupDatabase() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("setup.sql");
            Scanner s = new Scanner(is).useDelimiter("\\A");
            String sql = s.hasNext() ? s.next() : "";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println(">>> Tabela w bazie danych jest gotowa.");
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas tworzenia tabeli: " + e.getMessage());
        }
    }

    public void saveUser(String username, String plainPassword) {
        String sql = "INSERT INTO app_users (username, password_hash) VALUES (?, ?)";

        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            pstmt.executeUpdate();
            System.out.println(">>> Użytkownik '" + username + "' zapisany w bazie danych.");

        } catch (Exception e) {
            System.err.println("Błąd zapisu użytkownika: " + e.getMessage());
        }
    }
}