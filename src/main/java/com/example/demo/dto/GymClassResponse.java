package com.example.demo.dto;

import com.example.demo.model.enums.ClassStatus;

import java.time.LocalDateTime;

public record GymClassResponse(
        Long id,
        String name,
        String description,
        Long trainerId,
        String trainerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer capacity,
        long bookedCount,
        long freeSpots,
        String room,
        ClassStatus status
) {
}
