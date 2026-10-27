package com.example.demo.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Jednolity format odpowiedzi z błędem. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, Map.of());
    }
}
