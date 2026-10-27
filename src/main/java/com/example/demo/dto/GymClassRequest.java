package com.example.demo.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Tworzenie/edycja zajęć. Trener tworzy zajęcia dla siebie (trainerId ignorowane),
 * administrator musi wskazać trenera.
 */
public record GymClassRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        Long trainerId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull @Min(1) @Max(500) Integer capacity,
        @Size(max = 50) String room
) {
}
