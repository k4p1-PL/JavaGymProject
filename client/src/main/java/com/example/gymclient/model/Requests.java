package com.example.gymclient.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Obiekty wysyłane do API (odpowiadają DTO żądań na serwerze). */
public final class Requests {

    private Requests() {
    }

    public record Login(String email, String password) {
    }

    public record Register(String email, String password, String firstName, String lastName,
                           String phoneNumber, LocalDate dateOfBirth) {
    }

    public record UserCreate(String email, String password, String firstName, String lastName,
                             String phoneNumber, LocalDate dateOfBirth, String role) {
    }

    public record UserUpdate(String firstName, String lastName, String phoneNumber, LocalDate dateOfBirth,
                             String role, boolean active, String newPassword) {
    }

    public record Profile(String firstName, String lastName, String phoneNumber, LocalDate dateOfBirth) {
    }

    public record ChangePassword(String currentPassword, String newPassword) {
    }

    public record Plan(String name, String description, Integer durationDays, Integer entryLimit,
                       BigDecimal price, boolean includesClasses, boolean active) {
    }

    public record Purchase(Long planId, String paymentMethod, LocalDate startDate) {
    }

    public record Sell(Long userId, Long planId, String paymentMethod, LocalDate startDate) {
    }

    public record GymClass(String name, String description, Long trainerId, LocalDateTime startTime,
                           LocalDateTime endTime, Integer capacity, String room) {
    }

    public record Booking(Long gymClassId) {
    }

    public record CheckIn(Long userId, String email) {
    }
}
