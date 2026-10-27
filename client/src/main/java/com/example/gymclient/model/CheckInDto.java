package com.example.gymclient.model;

import java.time.LocalDateTime;

public record CheckInDto(Long id, Long userId, String memberName, Long membershipId, String planName,
                         Integer remainingEntries, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
}
