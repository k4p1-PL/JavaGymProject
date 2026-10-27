package com.example.demo.controller;

import com.example.demo.dto.CheckInRequest;
import com.example.demo.dto.CheckInResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
@Tag(name = "Wejścia (recepcja)")
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Rejestracja wejścia klubowicza (sprawdza ważność karnetu)")
    public CheckInResponse checkIn(@RequestBody CheckInRequest request) {
        return checkInService.checkIn(request);
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejestracja wyjścia")
    public CheckInResponse checkOut(@PathVariable Long id) {
        return checkInService.checkOut(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ostatnie wejścia (100)")
    public List<CheckInResponse> recent() {
        return checkInService.findRecent();
    }

    @GetMapping("/my")
    @Operation(summary = "Moja historia wejść")
    public List<CheckInResponse> my(@AuthenticationPrincipal AuthUser user) {
        return checkInService.findForUser(user.id());
    }
}
