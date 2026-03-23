package com.saasforge.service;

import com.saasforge.dto.LoginRequest;
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

    public String login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found for email: {}", request.getEmail());
                    return new ResourceNotFoundException("User not found");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for email: {}", request.getEmail());
            throw new BadRequestException("Invalid credentials");
        }

        log.info("Login successful for email: {}", request.getEmail());

        return jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getTenant().getId(),
                user.getRole().getRoleKey()
        );
    }
}
