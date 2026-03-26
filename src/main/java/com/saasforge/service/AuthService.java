package com.saasforge.service;

import com.saasforge.dto.AuthResponse;
import com.saasforge.dto.LoginRequest;
import com.saasforge.dto.RefreshTokenRequest;
import com.saasforge.entity.RefreshToken;
import com.saasforge.entity.User;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.UserRepository;
import com.saasforge.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getEmail());
                    return new ResourceNotFoundException("User not found");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for: {}", request.getEmail());
            throw new BadRequestException("Invalid credentials");
        }

        String accessToken = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getTenant().getId(),
                user.getRole().getRoleKey()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Login successful for email: {}", request.getEmail());
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        log.info("Refresh token request received");

        RefreshToken newRefreshToken = refreshTokenService.validateAndRotate(request.getRefreshToken());

        User user = newRefreshToken.getUser();

        String accessToken = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getTenant().getId(),
                user.getRole().getRoleKey()
        );

        log.info("Token refreshed for userId={}", user.getId());
        return new AuthResponse(accessToken, newRefreshToken.getToken());
    }
}
