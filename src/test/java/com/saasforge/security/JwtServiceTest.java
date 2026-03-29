package com.saasforge.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    // Must be 32+ bytes for HMAC-SHA256
    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-1234567890";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiry", 900000L);
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtService.generateToken("user@example.com", 1L, 10L, "ADMIN");
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        String token = jwtService.generateToken("user@example.com", 1L, 10L, "ADMIN");
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        String token = jwtService.generateToken("user@example.com", 42L, 10L, "ADMIN");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void extractTenantId_shouldReturnCorrectTenantId() {
        String token = jwtService.generateToken("user@example.com", 1L, 99L, "ADMIN");
        assertThat(jwtService.extractTenantId(token)).isEqualTo(99L);
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtService.generateToken("user@example.com", 1L, 10L, "ADMIN");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValid_shouldReturnTrueForFreshToken() {
        String token = jwtService.generateToken("user@example.com", 1L, 10L, "ADMIN");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForTamperedToken() {
        String token = jwtService.generateToken("user@example.com", 1L, 10L, "ADMIN");
        String tampered = token + "tampered";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForGarbageString() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }
}
