package com.example.demo.dto;

import com.example.demo.model.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate dateOfBirth,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
}
