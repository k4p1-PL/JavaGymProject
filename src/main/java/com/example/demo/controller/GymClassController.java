package com.example.demo.controller;

import com.example.demo.dto.BookingResponse;
import com.example.demo.dto.GymClassRequest;
import com.example.demo.dto.GymClassResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.GymClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Zajęcia grupowe", description = "Grafik zajęć; tworzenie i edycja – TRAINER (własne) lub ADMIN")
public class GymClassController {

    private final GymClassService gymClassService;

    @GetMapping
    @Operation(summary = "Grafik zajęć (domyślnie najbliższe 14 dni)")
    public List<GymClassResponse> schedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return gymClassService.findSchedule(from, to);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "Zajęcia prowadzone przez zalogowanego trenera")
    public List<GymClassResponse> my(@AuthenticationPrincipal AuthUser user) {
        return gymClassService.findForTrainer(user.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły zajęć")
    public GymClassResponse findById(@PathVariable Long id) {
        return gymClassService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dodanie zajęć do grafiku")
    public GymClassResponse create(@Valid @RequestBody GymClassRequest request, @AuthenticationPrincipal AuthUser user) {
        return gymClassService.create(request, user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Edycja zajęć")
    public GymClassResponse update(@PathVariable Long id, @Valid @RequestBody GymClassRequest request,
                                   @AuthenticationPrincipal AuthUser user) {
        return gymClassService.update(id, request, user);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Odwołanie zajęć (anuluje wszystkie rezerwacje)")
    public GymClassResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return gymClassService.cancel(id, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Usunięcie zajęć bez zapisów")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        gymClassService.delete(id, user);
    }

    @GetMapping("/{id}/bookings")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Lista zapisanych na zajęcia")
    public List<BookingResponse> participants(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return gymClassService.findParticipants(id, user);
    }
}
