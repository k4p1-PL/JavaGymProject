package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.enums.Role;
import com.example.demo.security.AuthUser;
import com.example.demo.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Użytkownicy", description = "Profil zalogowanego użytkownika i zarządzanie kontami (ADMIN)")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Dane zalogowanego użytkownika")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return userService.findById(user.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Edycja własnego profilu")
    public UserResponse updateMe(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(user.id(), request);
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Zmiana własnego hasła")
    public void changePassword(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.id(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista użytkowników (opcjonalnie filtr po roli)")
    public List<UserResponse> findAll(@RequestParam(required = false) Role role) {
        return userService.findAll(role);
    }

    @GetMapping("/trainers")
    @Operation(summary = "Lista trenerów (dla wszystkich zalogowanych)")
    public List<UserResponse> trainers() {
        return userService.findAll(Role.TRAINER).stream().filter(UserResponse::active).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Szczegóły użytkownika")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Utworzenie konta (dowolna rola)")
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edycja konta")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal AuthUser admin) {
        return userService.update(id, request, admin.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dezaktywacja konta (historia karnetów i wejść zostaje)")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        userService.deactivate(id, admin.id());
    }
}
