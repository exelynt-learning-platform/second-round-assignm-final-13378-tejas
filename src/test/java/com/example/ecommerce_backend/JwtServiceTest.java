package com.example.ecommerce_backend;

import com.example.ecommerce_backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    // 256-bit secret (32 chars minimum for HS256)
    private static final String SECRET = "testSecretKeyForJUnit1234567890AB";
    private static final long EXPIRY_MS = 3_600_000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRY_MS);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void isTokenValid_freshToken_returnsTrue() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken("user@example.com");
        String tampered = token + "tampered";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        JwtService shortLived = new JwtService(SECRET, 1); // 1ms expiry
        String token = shortLived.generateToken("user@example.com");

        // Wait for token to expire
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        assertThat(shortLived.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_emptyString_returnsFalse() {
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    void isTokenValid_randomString_returnsFalse() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }
}
