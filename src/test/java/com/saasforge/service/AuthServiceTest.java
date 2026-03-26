package com.saasforge.service;

import com.saasforge.dto.AuthResponse;
import com.saasforge.dto.LoginRequest;
import com.saasforge.dto.RefreshTokenRequest;
import com.saasforge.entity.RefreshToken;
import com.saasforge.entity.SystemRole;
import com.saasforge.entity.Tenant;
import com.saasforge.entity.User;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.UserRepository;
import com.saasforge.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User buildUser() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);

        SystemRole role = new SystemRole();
        role.setRoleKey("ADMIN");

        User user = new User();
        user.setId(10L);
        user.setEmail("admin@test.com");
        user.setPassword("encodedPassword");
        user.setTenant(tenant);
        user.setRole(role);
        return user;
    }

    private RefreshToken buildRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token-value");
        rt.setUser(user);
        return rt;
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsAuthResponse() {
        User user = buildUser();
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken("admin@test.com", 10L, 1L, "ADMIN")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(buildRefreshToken(user));

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-value");
    }

    @Test
    void login_success_callsGenerateTokenWithCorrectArgs() {
        User user = buildUser();
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyLong(), anyLong(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(buildRefreshToken(user));

        authService.login(request);

        verify(jwtService).generateToken("admin@test.com", 10L, 1L, "ADMIN");
    }

    @Test
    void login_success_createsRefreshToken() {
        User user = buildUser();
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyLong(), anyLong(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(buildRefreshToken(user));

        authService.login(request);

        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void login_userNotFound_throwsResourceNotFoundException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void login_wrongPassword_throwsBadRequestException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("wrongpassword");

        User user = buildUser();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_wrongPassword_doesNotGenerateToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("wrongpassword");

        User user = buildUser();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class);

        verify(jwtService, never()).generateToken(anyString(), anyLong(), anyLong(), anyString());
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_success_returnsNewAuthResponse() {
        User user = buildUser();
        RefreshToken rotatedToken = buildRefreshToken(user);
        rotatedToken.setToken("new-refresh-token");

        when(refreshTokenService.validateAndRotate("old-refresh-token")).thenReturn(rotatedToken);
        when(jwtService.generateToken("admin@test.com", 10L, 1L, "ADMIN")).thenReturn("new-access-token");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh-token");

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refresh_success_callsGenerateTokenWithUserDetails() {
        User user = buildUser();
        when(refreshTokenService.validateAndRotate("token")).thenReturn(buildRefreshToken(user));
        when(jwtService.generateToken(anyString(), anyLong(), anyLong(), anyString())).thenReturn("access");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        authService.refresh(request);

        verify(jwtService).generateToken("admin@test.com", 10L, 1L, "ADMIN");
    }

    @Test
    void refresh_invalidToken_throwsBadRequestException() {
        when(refreshTokenService.validateAndRotate("bad-token"))
                .thenThrow(new BadRequestException("Invalid refresh token"));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad-token");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid refresh token");
    }
}
