package com.example.demo.dto;

import java.math.BigDecimal;

/** Podsumowanie dla panelu administratora. */
public record StatsResponse(
        long activeMembers,
        long activeTrainers,
        long activeMemberships,
        long upcomingClasses,
        long checkInsToday,
        long currentlyInGym,
        BigDecimal revenueThisMonth
) {
}
