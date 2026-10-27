package com.example.demo.controller;

import com.example.demo.dto.PaymentResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Płatności")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Wszystkie płatności")
    public List<PaymentResponse> findAll() {
        return paymentService.findAll();
    }

    @GetMapping("/my")
    @Operation(summary = "Moje płatności")
    public List<PaymentResponse> my(@AuthenticationPrincipal AuthUser user) {
        return paymentService.findForUser(user.id());
    }
}
