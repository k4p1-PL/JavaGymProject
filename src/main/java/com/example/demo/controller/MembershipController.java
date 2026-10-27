package com.example.demo.controller;

import com.example.demo.dto.MembershipResponse;
import com.example.demo.dto.PurchaseRequest;
import com.example.demo.dto.SellMembershipRequest;
import com.example.demo.security.AuthUser;
import com.example.demo.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
@Tag(name = "Karnety klubowiczów", description = "Zakup, sprzedaż na recepcji i anulowanie karnetów")
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping("/purchase")
    @PreAuthorize("hasRole('MEMBER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Zakup karnetu przez zalogowanego klubowicza")
    public MembershipResponse purchase(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody PurchaseRequest request) {
        return membershipService.purchase(user.id(), request);
    }

    @GetMapping("/my")
    @Operation(summary = "Moje karnety")
    public List<MembershipResponse> my(@AuthenticationPrincipal AuthUser user) {
        return membershipService.findForUser(user.id());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Wszystkie karnety albo karnety wybranego klubowicza")
    public List<MembershipResponse> findAll(@RequestParam(required = false) Long userId) {
        return userId != null ? membershipService.findForUser(userId) : membershipService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sprzedaż karnetu na recepcji")
    public MembershipResponse sell(@Valid @RequestBody SellMembershipRequest request) {
        return membershipService.sell(request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Anulowanie karnetu (właściciel lub ADMIN)")
    public MembershipResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return membershipService.cancel(id, user);
    }
}
