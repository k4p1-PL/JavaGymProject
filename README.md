# GymManager – system do obsługi siłowni

Aplikacja klient-serwer do prowadzenia siłowni: konta z podziałem na role, sprzedaż karnetów, grafik zajęć grupowych, zapisy na zajęcia, rejestracja wejść na recepcji oraz import i eksport danych w formatach JSON i XML.

- **Serwer:** Spring Boot 4 (REST API), Spring Data JPA, MySQL, Spring Security + JWT
- **Klient:** aplikacja desktopowa JavaFX łącząca się z REST API
- **Dokumentacja API:** OpenAPI / Swagger UI

## Spełnione wymagania projektu

| Wymaganie | Realizacja |
|---|---|
| Architektura klient-serwer | Backend Spring Boot (`src/`) + osobny klient JavaFX (`client/`) komunikujący się przez HTTP/JSON |
| Trwałe przechowywanie danych, CRUD | MySQL + JPA; pełny CRUD m.in. dla użytkowników, rodzajów karnetów i zajęć |
| Bezpieczeństwo i uwierzytelnianie | Spring Security, tokeny JWT, role `ADMIN`, `TRAINER`, `MEMBER`, hasła szyfrowane BCrypt |
| Serializacja danych | Import i eksport oferty karnetów i grafiku zajęć w **JSON** (Jackson) i **XML** (JAXB) |
| Testy | Testy jednostkowe (JUnit 5 + Mockito) i integracyjne (`@SpringBootTest` + MockMvc + baza H2) |
| Dokumentacja | Ten plik, opis REST API w OpenAPI (Swagger UI), podział ról w zespole (niżej) |

## Funkcje według ról

**Klubowicz (`MEMBER`)**
- rejestracja konta i logowanie,
- przegląd oferty i zakup karnetu, historia karnetów i płatności,
- grafik zajęć, zapis i anulowanie zapisu (najpóźniej 2 godz. przed zajęciami),
- historia własnych wejść, edycja profilu, zmiana hasła.

**Trener (`TRAINER`)**
- dodawanie, edycja, odwoływanie i usuwanie własnych zajęć,
- lista uczestników i oznaczanie obecności.

**Administrator / recepcja (`ADMIN`)**
- panel ze statystykami (aktywni klubowicze, karnety, wejścia, przychód w miesiącu),
- recepcja: wpuszczanie klubowiczów (sprawdzenie ważności karnetu, zużycie wejścia) i rejestracja wyjść,
- zarządzanie kontami (dodawanie, edycja, zmiana roli, dezaktywacja),
- zarządzanie ofertą karnetów, sprzedaż i anulowanie karnetów, lista płatności,
- zarządzanie grafikiem zajęć wszystkich trenerów,
- import i eksport danych (JSON / XML).

### Najważniejsze reguły biznesowe

- Karnet jest ważny od daty początku przez `durationDays` dni; karnety z limitem wejść tracą jedno wejście przy każdym wejściu na siłownię.
- Przeterminowane karnety są codziennie (i przy starcie serwera) oznaczane jako `EXPIRED`.
- Zapis na zajęcia wymaga ważnego karnetu obejmującego zajęcia grupowe i wolnego miejsca.
- Odwołanie zajęć anuluje wszystkie zapisy; zajęć z zapisami nie można usunąć, tylko odwołać.
- Rodzaju karnetu, który ktoś już kupił, nie można usunąć – wycofuje się go ze sprzedaży.
- Usunięcie konta to dezaktywacja: historia karnetów, płatności i wejść zostaje.

## Wymagania

- JDK 17 lub nowszy (Gradle sam pobierze JDK 17 do kompilacji, jeśli go nie ma),
- MySQL 8 uruchomiony lokalnie na porcie 3306.

## Uruchomienie

### 1. Baza danych

Wystarczy działający serwer MySQL. Baza `gym` i tabele utworzą się automatycznie przy pierwszym starcie (`createDatabaseIfNotExist=true`, `ddl-auto=update`).

Domyślnie aplikacja łączy się jako `root` z pustym hasłem. Swoje hasło podaj w zmiennej środowiskowej albo wpisz je w `src/main/resources/application.properties` (`spring.datasource.password=`).

```powershell
# Windows PowerShell
$env:DB_PASSWORD = "twoje_haslo"
```

### 2. Serwer (backend)

```powershell
.\gradlew.bat bootRun
```

Albo w IntelliJ: uruchom klasę `DemoApplication`.

Serwer działa pod adresem `http://localhost:8080`. Przy pierwszym uruchomieniu na pustej bazie tworzone są dane demonstracyjne:

| Rola | Login | Hasło |
|---|---|---|
| Administrator | `admin@gym.pl` | `admin123` |
| Trener | `trener@gym.pl` | `trener123` |
| Trener | `joga@gym.pl` | `trener123` |
| Klubowicz (z aktywnym karnetem) | `jan@gym.pl` | `klient123` |
| Klubowicz (bez karnetu) | `ola@gym.pl` | `klient123` |

Tworzenie danych demo wyłącza `app.seed-demo-data=false`.

### 3. Klient JavaFX

Przy uruchomionym serwerze, w drugim terminalu:

```powershell
.\gradlew.bat :client:run
```

Albo w IntelliJ: uruchom klasę `com.example.gymclient.Launcher` z modułu `client`.

Klient łączy się domyślnie z `http://localhost:8080`; inny adres: `.\gradlew.bat :client:run -PapiUrl=http://adres:8080`.

## Dokumentacja REST API

Po uruchomieniu serwera:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Specyfikacja OpenAPI (JSON):** http://localhost:8080/v3/api-docs

Żeby wywoływać chronione endpointy w Swagger UI: wykonaj `POST /api/auth/login`, skopiuj `token` z odpowiedzi i wklej go w okno **Authorize**.

### Przegląd endpointów

| Metoda i ścieżka | Opis | Dostęp |
|---|---|---|
| `POST /api/auth/register` | Rejestracja klubowicza (zwraca token) | publiczny |
| `POST /api/auth/login` | Logowanie (zwraca token JWT) | publiczny |
| `GET/PUT /api/users/me`, `POST /api/users/me/password` | Własny profil, zmiana hasła | zalogowany |
| `GET/POST /api/users`, `GET/PUT/DELETE /api/users/{id}` | CRUD kont | ADMIN |
| `GET /api/users/trainers` | Lista trenerów | zalogowany |
| `GET /api/plans`, `GET /api/plans/{id}` | Oferta karnetów | publiczny |
| `POST/PUT/DELETE /api/plans…` | Zarządzanie ofertą | ADMIN |
| `POST /api/memberships/purchase` | Zakup karnetu | MEMBER |
| `GET /api/memberships/my` | Moje karnety | zalogowany |
| `GET/POST /api/memberships` | Wszystkie karnety, sprzedaż na recepcji | ADMIN |
| `POST /api/memberships/{id}/cancel` | Anulowanie karnetu | właściciel / ADMIN |
| `GET /api/payments`, `GET /api/payments/my` | Płatności | ADMIN / zalogowany |
| `GET /api/classes?from=&to=` | Grafik zajęć | zalogowany |
| `GET /api/classes/my` | Moje zajęcia | TRAINER |
| `POST/PUT/DELETE /api/classes…`, `POST /api/classes/{id}/cancel` | Zarządzanie zajęciami | TRAINER (własne) / ADMIN |
| `GET /api/classes/{id}/bookings` | Uczestnicy zajęć | TRAINER (własne) / ADMIN |
| `POST /api/bookings`, `GET /api/bookings/my` | Zapis na zajęcia, moje zapisy | MEMBER |
| `POST /api/bookings/{id}/cancel` | Anulowanie zapisu | właściciel / ADMIN |
| `POST /api/bookings/{id}/attendance?attended=` | Obecność | TRAINER / ADMIN |
| `POST /api/checkins`, `POST /api/checkins/{id}/checkout`, `GET /api/checkins` | Recepcja | ADMIN |
| `GET /api/checkins/my` | Moje wejścia | zalogowany |
| `GET /api/stats` | Statystyki | ADMIN |
| `GET /api/transfer/export?format=json\|xml` | Eksport danych do pliku | ADMIN |
| `POST /api/transfer/import` (`application/json` lub `application/xml`) | Import danych | ADMIN |

Błędy zwracane są w jednolitym formacie JSON (`status`, `error`, `message`, `path`, `fieldErrors`); kody: 400 – niepoprawne dane, 401 – brak/niepoprawny token, 403 – brak uprawnień, 404 – brak zasobu, 409 – naruszenie reguły biznesowej.

### Import i eksport

Plik zawiera ofertę karnetów i grafik zajęć. Przykłady: `przyklady/gym-data-przyklad.json` i `przyklady/gym-data-przyklad.xml`.

- Karnety są dopasowywane po nazwie: istniejący jest aktualizowany, nowy – dodawany.
- Zajęcia są dodawane, jeśli w grafiku nie ma jeszcze zajęć o tej samej nazwie, terminie i trenerze. Trener jest wskazany adresem email i musi istnieć w systemie.
- Błędne pozycje są pomijane, a ich opis trafia do pola `errors` w odpowiedzi.

## Testy

```powershell
.\gradlew.bat test
```

Testy nie wymagają MySQL – używają bazy H2 w pamięci (`src/test/resources/application.properties`).

- **Jednostkowe** (`src/test/java/.../service`, `.../security`, `.../transfer`): logika zapisów na zajęcia, zakupu i anulowania karnetów, rejestracji wejść, tokenów JWT oraz konwersji JSON/XML; zależności zastąpione mockami (Mockito).
- **Integracyjne** (`src/test/java/.../integration`): pełna aplikacja z bazą H2 i MockMvc – rejestracja i logowanie, kontrola dostępu (401/403), walidacja, pełny scenariusz „karnet → zapis na zajęcia → wejście”, CRUD karnetów, import/eksport JSON i XML.

Raport z testów: `build/reports/tests/test/index.html`.

## Struktura projektu

```
demo/
├── build.gradle, settings.gradle      – build Gradle (projekt główny = serwer, podprojekt client)
├── src/main/java/com/example/demo/
│   ├── model/          – encje JPA i enumy
│   ├── repository/     – repozytoria Spring Data
│   ├── dto/            – obiekty żądań i odpowiedzi API + mapper
│   ├── service/        – logika biznesowa
│   ├── controller/     – kontrolery REST
│   ├── security/       – JWT, filtr uwierzytelniania, konfiguracja Spring Security
│   ├── transfer/       – import/eksport JSON i XML
│   ├── exception/      – wyjątki i globalna obsługa błędów
│   └── config/         – OpenAPI, dane demo, harmonogram wygaszania karnetów, zegar
├── src/test/           – testy jednostkowe i integracyjne
├── client/             – klient desktopowy JavaFX
│   └── src/main/java/com/example/gymclient/
│       ├── api/        – klient HTTP REST API
│       ├── model/      – rekordy odpowiadające DTO serwera
│       ├── ui/         – ekran logowania, okno główne i zakładki
│       └── util/       – pomocnicy (tabele, formularze, okna dialogowe)
└── przyklady/          – przykładowe pliki do importu
```

### Model danych

- `User` – konto (email, hasło, dane osobowe, rola, aktywność),
- `MembershipPlan` – rodzaj karnetu w ofercie (okres, cena, limit wejść, czy obejmuje zajęcia),
- `Membership` – karnet kupiony przez klubowicza (daty, pozostałe wejścia, status),
- `Payment` – płatność za karnet,
- `GymClass` – zajęcia w grafiku (trener, termin, limit miejsc, sala, status),
- `Booking` – zapis klubowicza na zajęcia,
- `CheckIn` – wejście na siłownię (godzina wejścia i wyjścia).

## Podział ról w zespole

> Uzupełnij przed oddaniem projektu.

| Osoba | Zakres prac |
|---|---|
| Imię Nazwisko | np. backend: model danych, REST API, bezpieczeństwo |
| Imię Nazwisko | np. klient JavaFX, import/eksport, testy, dokumentacja |
