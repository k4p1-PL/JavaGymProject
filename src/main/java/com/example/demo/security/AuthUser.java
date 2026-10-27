package com.example.demo.security;

import com.example.demo.model.enums.Role;

/** Zalogowany użytkownik odczytany z tokenu JWT (principal w SecurityContext). */
public record AuthUser(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
