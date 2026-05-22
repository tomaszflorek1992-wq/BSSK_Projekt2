# BSSK_Projekt2

Aplikacja to konsolowy system bezpiecznej rejestracji użytkowników w bazie danych. Wykorzystuje dwie warstwy ochrony: **szyfrowanie symetryczne AES** (ochrona danych bazy) oraz **funkcję skrótu BCrypt** (ochrona haseł).

---

## 🛠️ Architektura i Kluczowe Komponenty

1. **`Main.java`** – Punkt startowy programu. Steruje przepływem danych, komunikuje się z użytkownikiem przez konsolę i wywołuje logikę biznesową.
2. **`DatabaseManager.java`** – Odpowiada za konfigurację połączenia, automatyczne przygotowanie struktury bazy danych (`setup.sql`) oraz bezpieczny zapis użytkownika.
3. **`EncryptionTool.java`** – Narzędzie kryptograficzne realizujące szyfrowanie i deszyfrowanie algorytmem **AES**.

---

## 🔄 Przepływ Programu Krok po Kroku

### Krok 1: Bezpieczne ładowanie konfiguracji
Program nie przechowuje jawnych haseł do bazy danych w kodzie ani w plikach konfiguracyjnych. 
* W konstruktorze `DatabaseManager` wczytywany jest plik `application.properties`.
* Program pobiera z systemu operacyjnego klucz tajny: `System.getenv("MASTER_KEY")`.
* Zaszyfrowany login i hasło z pliku properties są deszyfrowane w pamięci RAM za pomocą `EncryptionTool.decrypt()`.

### Krok 2: Przygotowanie bazy danych (Setup)
* Wywoływana jest metoda `dbManager.setupDatabase()`.
* Program wczytuje plik skryptu `setup.sql` i wykonuje go w bazie danych.
* Tworzona jest tabela `app_users` (jeśli jeszcze nie istnieje), co zapobiega błędom braku struktury.

### Krok 3: Pobranie danych od użytkownika
* W konsoli pojawia się monit o podanie nazwy użytkownika oraz hasła.
* Dane są przechwytywane strumieniem `Scanner(System.in)`.

### Krok 4: Haszowanie i bezpieczny zapis
* Hasło w czystej postaci **nigdy** nie trafia do bazy danych. Jest nieodwracalnie haszowane algorytmem BCrypt wraz z automatycznie generowaną solą:
  `BCrypt.hashpw(plainPassword, BCrypt.gensalt())`
* Dane są zapisywane do bazy poprzez `PreparedStatement` chroniący aplikację przed atakami typu **SQL Injection**:
  `INSERT INTO app_users (username, password_hash) VALUES (?, ?)`

---

## 🔒 Zastosowane Mechanizmy Bezpieczeństwa

* **Separation of Secrets:** Dane dostępowe do bazy są zaszyfrowane (AES). Do ich odczytu wymagany jest `MASTER_KEY` ustawiony jako zmienna środowiskowa systemu.
* **Ochrona haseł (BCrypt):** W bazie ląduje bezpieczny hasz. W przypadku wycieku bazy danych, hasła użytkowników pozostają bezpieczne.
* **Odporność na SQL Injection:** Wykorzystanie sparametryzowanych zapytań (`PreparedStatement`) zamiast zwykłego łączenia stringów w SQL.