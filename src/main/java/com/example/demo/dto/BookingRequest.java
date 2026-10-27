package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public record BookingRequest(@NotNull Long gymClassId) {
}
