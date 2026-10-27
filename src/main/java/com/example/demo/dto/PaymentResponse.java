package com.example.demo.dto;

import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long membershipId,
        Long userId,
        String memberName,
        String planName,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime paidAt
) {
}
