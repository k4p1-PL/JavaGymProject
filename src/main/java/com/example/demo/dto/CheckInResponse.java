package com.example.demo.dto;

import java.time.LocalDateTime;

public record CheckInResponse(
        Long id,
        Long userId,
        String memberName,
        Long membershipId,
        String planName,
        Integer remainingEntries,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime
) {
}
