package com.example.demo.dto;

import com.example.demo.model.enums.MembershipStatus;

import java.time.LocalDate;

public record MembershipResponse(
        Long id,
        Long userId,
        String memberName,
        String memberEmail,
        Long planId,
        String planName,
        boolean includesClasses,
        LocalDate startDate,
        LocalDate endDate,
        Integer remainingEntries,
        MembershipStatus status,
        boolean validToday
) {
}
