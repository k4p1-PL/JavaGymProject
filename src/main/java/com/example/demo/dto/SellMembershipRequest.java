package com.example.demo.dto;

import com.example.demo.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Sprzedaż karnetu na recepcji (administrator wybiera klubowicza). */
public record SellMembershipRequest(
        @NotNull Long userId,
        @NotNull Long planId,
        @NotNull PaymentMethod paymentMethod,
        LocalDate startDate
) {
}
