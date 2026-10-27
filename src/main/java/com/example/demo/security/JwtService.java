package com.example.demo.security;

import com.example.demo.model.User;
import com.example.demo.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/** Generowanie i weryfikacja tokenów JWT (HS256). */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(JwtProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret musi mieć co najmniej 32 znaki");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expirationMinutes = properties.expirationMinutes();
    }

    public Instant expirationFromNow() {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }

    public String generateToken(User user, Instant expiresAt) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    /** Zwraca dane użytkownika z poprawnego tokenu albo pusty Optional, gdy token jest zły lub wygasł. */
    public Optional<AuthUser> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = ((Number) claims.get(CLAIM_USER_ID)).longValue();
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return Optional.of(new AuthUser(id, claims.getSubject(), role));
        } catch (JwtException | IllegalArgumentException | NullPointerException | ClassCastException e) {
            return Optional.empty();
        }
    }
}
