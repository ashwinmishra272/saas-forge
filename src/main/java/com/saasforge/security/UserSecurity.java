package com.saasforge.security;

import com.saasforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = (String) authentication.getPrincipal();

        return userRepository.findById(userId)
                .map(user -> user.getEmail().equals(currentEmail))
                .orElse(false);
    }
}
