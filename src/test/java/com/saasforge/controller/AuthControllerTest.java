package com.saasforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasforge.dto.AuthResponse;
import com.saasforge.dto.LoginRequest;
import com.saasforge.dto.RefreshTokenRequest;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.GlobalExceptionHandler;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private LoginRequest validLoginRequest() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("password123");
        return req;
    }

    private AuthResponse sampleAuthResponse() {
        return new AuthResponse("access-token", "refresh-token");
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        when(authService.login(any())).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_userNotFound_returns404() throws Exception {
        when(authService.login(any())).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void login_wrongPassword_returns400() throws Exception {
        when(authService.login(any())).thenThrow(new BadRequestException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_invalidEmail_returns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("not-an-email");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_nullEmail_returns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(null);
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/refresh ────────────────────────────────────────────────

    @Test
    void refresh_validToken_returns200WithNewTokens() throws Exception {
        when(authService.refresh(any())).thenReturn(new AuthResponse("new-access", "new-refresh"));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("old-refresh-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_invalidToken_returns400() throws Exception {
        when(authService.refresh(any())).thenThrow(new BadRequestException("Invalid refresh token"));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("bad-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"));
    }

    @Test
    void refresh_revokedToken_returns400() throws Exception {
        when(authService.refresh(any())).thenThrow(new BadRequestException("Refresh token has been revoked"));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("revoked-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Refresh token has been revoked"));
    }

    @Test
    void refresh_expiredToken_returns400() throws Exception {
        when(authService.refresh(any())).thenThrow(new BadRequestException("Refresh token has expired"));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("expired-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Refresh token has expired"));
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_nullToken_returns400() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(null);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
