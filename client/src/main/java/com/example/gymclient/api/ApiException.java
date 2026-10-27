package com.example.gymclient.api;

/** Błąd zwrócony przez serwer (komunikat z pola "message") albo błąd połączenia. */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
