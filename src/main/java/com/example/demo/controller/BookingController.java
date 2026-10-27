package com.example.demo.controller;

import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.BookingResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.BookingService;
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
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Rezerwacje zajęć")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Zapis na zajęcia (wymaga ważnego karnetu z zajęciami)")
    public BookingResponse book(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody BookingRequest request) {
        return bookingService.book(user.id(), request.gymClassId());
    }

    @GetMapping("/my")
    @Operation(summary = "Moje rezerwacje")
    public List<BookingResponse> my(@AuthenticationPrincipal AuthUser user) {
        return bookingService.findForUser(user.id());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Anulowanie rezerwacji (najpóźniej 2 godz. przed zajęciami)")
    public BookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return bookingService.cancel(id, user);
    }

    @PostMapping("/{id}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Oznaczenie obecności na zajęciach")
    public BookingResponse attendance(@PathVariable Long id, @RequestParam boolean attended,
                                      @AuthenticationPrincipal AuthUser user) {
        return bookingService.markAttendance(id, attended, user);
    }
}
