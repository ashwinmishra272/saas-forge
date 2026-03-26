package com.saasforge.service;

import com.saasforge.entity.RefreshToken;
import com.saasforge.entity.User;
import com.saasforge.exception.BadRequestException;
import com.saasforge.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(User user) {

        // Revoke all existing refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for userId={}", user.getId());
        return saved;
    }

    @Transactional
    public RefreshToken validateAndRotate(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            log.warn("Revoked refresh token used for userId={}", refreshToken.getUser().getId());
            throw new BadRequestException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired refresh token used for userId={}", refreshToken.getUser().getId());
            throw new BadRequestException("Refresh token has expired");
        }

        // Revoke the used token and issue a new one — token rotation
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return createRefreshToken(refreshToken.getUser());
    }
}
