package com.example.demo.dto;

/** Rejestracja wejścia na recepcji – podaj id klubowicza albo jego email. */
public record CheckInRequest(Long userId, String email) {
}
