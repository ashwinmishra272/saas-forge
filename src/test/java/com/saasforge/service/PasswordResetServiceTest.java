package com.saasforge.service;

import com.saasforge.dto.ForgotPasswordRequest;
import com.saasforge.dto.ResetPasswordRequest;
import com.saasforge.entity.PasswordResetToken;
import com.saasforge.entity.User;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.PasswordResetTokenRepository;
import com.saasforge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.InOrder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setPassword("encodedPassword");
        return user;
    }

    private PasswordResetToken buildValidToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return token;
    }

    private ForgotPasswordRequest forgotRequest() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("user@test.com");
        return req;
    }


    @Test
    void forgotPassword_emailNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.forgotPassword(forgotRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("user@test.com");
    }

    @Test
    void forgotPassword_invalidatesExistingTokensFirst() {
        User user = buildUser();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        passwordResetService.forgotPassword(forgotRequest());

        InOrder inOrder = inOrder(passwordResetTokenRepository);
        inOrder.verify(passwordResetTokenRepository).invalidateAllUserTokens(user);
        inOrder.verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPassword_savesTokenWithCorrectFields() {
        User user = buildUser();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        passwordResetService.forgotPassword(forgotRequest());

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());

        PasswordResetToken saved = captor.getValue();
        assertThat(saved.getToken()).isNotNull().isNotBlank();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isNotNull();
    }

    @Test
    void forgotPassword_tokenExpiresInApproximately15Minutes() {
        User user = buildUser();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        LocalDateTime before = LocalDateTime.now();
        passwordResetService.forgotPassword(forgotRequest());
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());

        LocalDateTime expiresAt = captor.getValue().getExpiresAt();
        assertThat(expiresAt).isAfter(before.plusMinutes(14));
        assertThat(expiresAt).isBefore(after.plusMinutes(16));
    }

    @Test
    void forgotPassword_sendsEmailWithSavedToken() {
        User user = buildUser();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        passwordResetService.forgotPassword(forgotRequest());

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());

        String savedToken = captor.getValue().getToken();
        verify(notificationProducer).publishPasswordReset("user@test.com", savedToken);
    }

    @Test
    void forgotPassword_doesNotSave_whenUserNotFound() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.forgotPassword(forgotRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(passwordResetTokenRepository, never()).save(any());
    }


    @Test
    void resetPassword_invalidToken_throwsBadRequestException() {
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("bad-token");
        request.setNewPassword("newpassword123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid reset token");
    }

    @Test
    void resetPassword_alreadyUsedToken_throwsBadRequestException() {
        User user = buildUser();
        PasswordResetToken usedToken = buildValidToken(user);
        usedToken.setUsed(true);

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("newpassword123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reset token has already been used");
    }

    @Test
    void resetPassword_expiredToken_throwsBadRequestException() {
        User user = buildUser();
        PasswordResetToken expiredToken = buildValidToken(user);
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newpassword123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reset token has expired");
    }

    @Test
    void resetPassword_success_encodesAndSavesNewPassword() {
        User user = buildUser();
        PasswordResetToken token = buildValidToken(user);
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpassword123")).thenReturn("encodedNewPassword");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newpassword123");

        passwordResetService.resetPassword(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedNewPassword");
    }

    @Test
    void resetPassword_success_marksTokenAsUsed() {
        User user = buildUser();
        PasswordResetToken token = buildValidToken(user);
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode(any())).thenReturn("encodedNewPassword");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newpassword123");

        passwordResetService.resetPassword(request);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository, atLeastOnce()).save(tokenCaptor.capture());

        boolean anyTokenMarkedUsed = tokenCaptor.getAllValues().stream()
                .anyMatch(PasswordResetToken::isUsed);
        assertThat(anyTokenMarkedUsed).isTrue();
    }

    @Test
    void resetPassword_success_invalidatesOtherUserTokens() {
        User user = buildUser();
        PasswordResetToken token = buildValidToken(user);
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode(any())).thenReturn("encodedNewPassword");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newpassword123");

        passwordResetService.resetPassword(request);

        verify(passwordResetTokenRepository).invalidateAllUserTokens(user);
    }

    @Test
    void resetPassword_expiredToken_doesNotSaveUser() {
        User user = buildUser();
        PasswordResetToken expiredToken = buildValidToken(user);
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newpassword123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_alreadyUsedToken_doesNotSaveUser() {
        User user = buildUser();
        PasswordResetToken usedToken = buildValidToken(user);
        usedToken.setUsed(true);
        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("newpassword123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}
