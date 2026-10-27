package com.example.gymclient.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Formatowanie wartości do wyświetlenia w tabelach. */
public final class Fmt {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("ADMIN", "Administrator"), Map.entry("TRAINER", "Trener"), Map.entry("MEMBER", "Klubowicz"),
            Map.entry("ACTIVE", "Aktywny"), Map.entry("EXPIRED", "Wygasły"), Map.entry("CANCELLED", "Anulowany"),
            Map.entry("SCHEDULED", "Zaplanowane"), Map.entry("COMPLETED", "Zakończone"),
            Map.entry("BOOKED", "Zapisany"), Map.entry("ATTENDED", "Obecny"), Map.entry("NO_SHOW", "Nieobecny"),
            Map.entry("PENDING", "Oczekuje"), Map.entry("FAILED", "Nieudana"), Map.entry("REFUNDED", "Zwrócona"),
            Map.entry("CASH", "Gotówka"), Map.entry("CARD", "Karta"), Map.entry("TRANSFER", "Przelew"),
            Map.entry("ONLINE", "Online"));

    private Fmt() {
    }

    public static String date(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "" : DATE_TIME.format(d);
    }

    public static String money(BigDecimal amount) {
        return amount == null ? "" : String.format("%.2f zł", amount);
    }

    public static String label(String code) {
        return code == null ? "" : LABELS.getOrDefault(code, code);
    }

    public static String orDash(Object value) {
        return value == null ? "—" : value.toString();
    }
}
