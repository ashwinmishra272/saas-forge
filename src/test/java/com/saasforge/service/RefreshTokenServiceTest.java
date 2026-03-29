package com.saasforge.service;

import com.saasforge.entity.RefreshToken;
import com.saasforge.entity.User;
import com.saasforge.exception.BadRequestException;
import com.saasforge.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@test.com");
        return user;
    }

    private RefreshToken buildToken(User user, boolean revoked, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setToken("some-token");
        token.setUser(user);
        token.setRevoked(revoked);
        token.setExpiresAt(expiresAt);
        return token;
    }

    // ── createRefreshToken ─────────────────────────────────────────────────────

    @Test
    void createRefreshToken_revokesExistingTokensForUser() {
        User user = buildUser(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.createRefreshToken(user);

        verify(refreshTokenRepository).revokeAllUserTokens(user);
    }

    @Test
    void createRefreshToken_savesNewToken() {
        User user = buildUser(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.createRefreshToken(user);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_returnsTokenWithCorrectUser() {
        User user = buildUser(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertThat(result.getUser()).isEqualTo(user);
    }

    @Test
    void createRefreshToken_generatesNonNullToken() {
        User user = buildUser(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertThat(result.getToken()).isNotNull().isNotEmpty();
    }

    @Test
    void createRefreshToken_setsExpiryInFuture() {
        User user = buildUser(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void createRefreshToken_setsRevokedFalse() {
        User user = buildUser(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertThat(result.isRevoked()).isFalse();
    }

    // ── validateAndRotate ──────────────────────────────────────────────────────

    @Test
    void validateAndRotate_invalidToken_throwsBadRequestException() {
        when(refreshTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("bad-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void validateAndRotate_revokedToken_throwsBadRequestException() {
        User user = buildUser(1L);
        RefreshToken token = buildToken(user, true, LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("revoked-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Refresh token has been revoked");
    }

    @Test
    void validateAndRotate_expiredToken_throwsBadRequestException() {
        User user = buildUser(1L);
        RefreshToken token = buildToken(user, false, LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("expired-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Refresh token has expired");
    }

    @Test
    void validateAndRotate_success_revokesOldToken() {
        User user = buildUser(1L);
        RefreshToken token = buildToken(user, false, LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.validateAndRotate("valid-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeastOnce()).save(captor.capture());

        boolean oldTokenRevoked = captor.getAllValues().stream()
                .anyMatch(t -> t.getToken().equals("some-token") && t.isRevoked());
        assertThat(oldTokenRevoked).isTrue();
    }

    @Test
    void validateAndRotate_success_returnsNewToken() {
        User user = buildUser(1L);
        RefreshToken token = buildToken(user, false, LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.validateAndRotate("valid-token");

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
    }

    @Test
    void validateAndRotate_success_newTokenIsNotRevoked() {
        User user = buildUser(1L);
        RefreshToken token = buildToken(user, false, LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.validateAndRotate("valid-token");

        assertThat(result.isRevoked()).isFalse();
    }
}
