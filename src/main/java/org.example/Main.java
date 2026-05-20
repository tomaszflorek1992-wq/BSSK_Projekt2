package org.example;

import java.util.Scanner;

public class Main {
    public static void main (String[] args) {
        try {
            DatabaseManager dbManager = new DatabaseManager();

            dbManager.setupDatabase();

            Scanner scanner = new Scanner(System.in);
            System.out.println("=== SYSTEM BEZPIECZNEJ REJESTRACJI ===");

            System.out.print("Podaj nazwe uzytkownika do rejestracji: ");
            String username = scanner.nextLine();

            System.out.print("Podaj haslo: ");
            String password = scanner.nextLine();

            dbManager.saveUser(username, password);

            System.out.println("=== Koniec programu ===");

        } catch (Exception e) {
            System.err.println("BŁĄD KRYTYCZNY: " + e.getMessage());
            System.err.println("Upewnij się, że ustawiono MASTER_KEY w zmiennych srodowiskowych!");
        }
    }
}
