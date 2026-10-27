package com.example.gymclient.model;

import java.time.LocalDateTime;

public record BookingDto(Long id, Long userId, String memberName, Long gymClassId, String className,
                         LocalDateTime classStartTime, String trainerName, String room, LocalDateTime bookedAt,
                         String status) {
}
