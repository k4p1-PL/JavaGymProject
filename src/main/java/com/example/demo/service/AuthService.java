package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.DtoMapper;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.User;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Rejestracja nowego klubowicza (rola MEMBER) i od razu zalogowanie. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Użytkownik z adresem " + email + " już istnieje");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .phoneNumber(req.phoneNumber())
                .dateOfBirth(req.dateOfBirth())
                .role(Role.MEMBER)
                .active(true)
                .build();
        user = userRepository.save(user);
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Niepoprawny email lub hasło"));
        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Konto jest zablokowane");
        }
        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        Instant expiresAt = jwtService.expirationFromNow();
        String token = jwtService.generateToken(user, expiresAt);
        return new AuthResponse(token, "Bearer", expiresAt, DtoMapper.toResponse(user));
    }
}
