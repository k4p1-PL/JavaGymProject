package com.example.demo.dto;

import java.math.BigDecimal;

public record PlanResponse(
        Long id,
        String name,
        String description,
        Integer durationDays,
        Integer entryLimit,
        BigDecimal price,
        boolean includesClasses,
        boolean active
) {
}
