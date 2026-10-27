package com.example.gymclient;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punkt startowy klienta. Osobna klasa (niedziedzicząca po Application) pozwala
 * uruchomić JavaFX z classpath, bez konfigurowania modułów.
 */
public class Launcher {

    /** Trzymamy referencję, żeby ustawienie loggera nie zostało usunięte przez GC. */
    private static final Logger JAVAFX_STARTUP_LOGGER = Logger.getLogger("com.sun.javafx.application.PlatformImpl");

    public static void main(String[] args) {

        JAVAFX_STARTUP_LOGGER.setLevel(Level.SEVERE);
        GymClientApp.main(args);
    }
}
