package com.example.gymclient.model;

import java.math.BigDecimal;

public record StatsDto(long activeMembers, long activeTrainers, long activeMemberships, long upcomingClasses,
                       long checkInsToday, long currentlyInGym, BigDecimal revenueThisMonth) {
}
