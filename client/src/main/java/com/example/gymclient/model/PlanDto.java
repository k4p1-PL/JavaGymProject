package com.example.gymclient.model;

import java.math.BigDecimal;

public record PlanDto(Long id, String name, String description, Integer durationDays, Integer entryLimit,
                      BigDecimal price, boolean includesClasses, boolean active) {

    @Override
    public String toString() {
        return name + " – " + price + " zł / " + durationDays + " dni";
    }
}
