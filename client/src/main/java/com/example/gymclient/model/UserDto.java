package com.example.gymclient.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserDto(Long id, String email, String firstName, String lastName, String phoneNumber,
                      LocalDate dateOfBirth, String role, boolean active, LocalDateTime createdAt) {

    public String fullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return fullName() + " (" + email + ")";
    }
}
