package com.example.demo.controller;

import com.example.demo.dto.PlanRequest;
import com.example.demo.dto.PlanResponse;
import com.example.demo.service.MembershipPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Tag(name = "Rodzaje karnetów", description = "Oferta karnetów (odczyt publiczny, zmiany – ADMIN)")
public class MembershipPlanController {

    private final MembershipPlanService planService;

    @GetMapping
    @Operation(summary = "Lista karnetów (domyślnie tylko w sprzedaży)")
    public List<PlanResponse> findAll(@RequestParam(defaultValue = "true") boolean onlyActive) {
        return planService.findAll(onlyActive);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły karnetu")
    public PlanResponse findById(@PathVariable Long id) {
        return planService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dodanie rodzaju karnetu")
    public PlanResponse create(@Valid @RequestBody PlanRequest request) {
        return planService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edycja rodzaju karnetu")
    public PlanResponse update(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        return planService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Usunięcie rodzaju karnetu (tylko jeśli nikt go nie kupił)")
    public void delete(@PathVariable Long id) {
        planService.delete(id);
    }
}
