package com.saasforge.security;

import com.saasforge.entity.User;
import com.saasforge.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSecurityTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSecurity userSecurity;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String email) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    // ── isCurrentUser ──────────────────────────────────────────────────────────

    @Test
    void isCurrentUser_matchingEmailAndId_returnsTrue() {
        setAuthentication("alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "alice@test.com")));

        assertThat(userSecurity.isCurrentUser(1L)).isTrue();
    }

    @Test
    void isCurrentUser_differentEmail_returnsFalse() {
        setAuthentication("bob@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "alice@test.com")));

        assertThat(userSecurity.isCurrentUser(1L)).isFalse();
    }

    @Test
    void isCurrentUser_userNotFound_returnsFalse() {
        setAuthentication("alice@test.com");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userSecurity.isCurrentUser(99L)).isFalse();
    }

    @Test
    void isCurrentUser_emailCaseSensitive_returnsFalse() {
        setAuthentication("Alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "alice@test.com")));

        assertThat(userSecurity.isCurrentUser(1L)).isFalse();
    }
}
