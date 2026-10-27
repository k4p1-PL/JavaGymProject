package com.example.demo.dto;

import com.example.demo.model.enums.PaymentMethod;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Zakup karnetu przez klubowicza. Brak startDate = od dziś. */
public record PurchaseRequest(
        @NotNull Long planId,
        @NotNull PaymentMethod paymentMethod,
        @FutureOrPresent LocalDate startDate
) {
}
