package com.example.gymclient.model;

import java.time.LocalDateTime;

public record GymClassDto(Long id, String name, String description, Long trainerId, String trainerName,
                          LocalDateTime startTime, LocalDateTime endTime, Integer capacity, long bookedCount,
                          long freeSpots, String room, String status) {
}
