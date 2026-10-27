package com.example.demo.security;

import com.example.demo.model.User;
import com.example.demo.model.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService(new JwtProperties("sekret-do-testow-jednostkowych-jwt-0123456789", 60));

    private final User user = User.builder().id(42L).email("jan@gym.pl").role(Role.MEMBER).build();

    @Test
    void generatedTokenCanBeParsedBack() {
        String token = jwtService.generateToken(user, jwtService.expirationFromNow());

        assertThat(jwtService.parseToken(token))
                .contains(new AuthUser(42L, "jan@gym.pl", Role.MEMBER));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user, jwtService.expirationFromNow());
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(jwtService.parseToken(tampered)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.generateToken(user, Instant.now().minusSeconds(60));

        assertThat(jwtService.parseToken(token)).isEmpty();
    }

    @Test
    void tokenSignedWithOtherSecretIsRejected() {
        JwtService other = new JwtService(new JwtProperties("zupelnie-inny-sekret-do-testow-0123456789", 60));
        String token = other.generateToken(user, other.expirationFromNow());

        assertThat(jwtService.parseToken(token)).isEmpty();
    }

    @Test
    void tooShortSecretIsNotAccepted() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("krotki", 60)))
                .isInstanceOf(IllegalStateException.class);
    }
}
