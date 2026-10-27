package com.example.demo.dto;

import com.example.demo.model.enums.Role;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Tworzenie użytkownika przez administratora (dowolna rola). */
public record UserCreateRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 100, message = "Hasło musi mieć od 6 do 100 znaków") String password,
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @Size(max = 20) String phoneNumber,
        @Past LocalDate dateOfBirth,
        @NotNull Role role
) {
}
