package com.example.gymclient.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDto(Long id, Long membershipId, Long userId, String memberName, String planName,
                         BigDecimal amount, String method, String status, LocalDateTime paidAt) {
}
