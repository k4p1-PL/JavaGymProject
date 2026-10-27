package com.example.gymclient.model;

import java.time.LocalDate;

public record MembershipDto(Long id, Long userId, String memberName, String memberEmail, Long planId, String planName,
                            boolean includesClasses, LocalDate startDate, LocalDate endDate, Integer remainingEntries,
                            String status, boolean validToday) {
}
