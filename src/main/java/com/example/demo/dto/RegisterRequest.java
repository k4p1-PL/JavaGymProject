package com.example.demo.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Samodzielna rejestracja klubowicza. */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 100, message = "Hasło musi mieć od 6 do 100 znaków") String password,
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @Size(max = 20) String phoneNumber,
        @Past LocalDate dateOfBirth
) {
}
