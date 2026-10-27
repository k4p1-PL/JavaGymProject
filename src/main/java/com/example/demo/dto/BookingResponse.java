package com.example.demo.dto;

import com.example.demo.model.enums.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        String memberName,
        Long gymClassId,
        String className,
        LocalDateTime classStartTime,
        String trainerName,
        String room,
        LocalDateTime bookedAt,
        BookingStatus status
) {
}
