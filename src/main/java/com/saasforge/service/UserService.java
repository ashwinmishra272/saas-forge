package com.saasforge.service;

import com.saasforge.dto.PageResponse;
import com.saasforge.dto.UpdateUserRequest;
import com.saasforge.dto.UserResponse;
import com.saasforge.entity.User;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public PageResponse<UserResponse> getAllUsers(int page, int size, String sortBy) {
        log.info("Fetching users - page={}, size={}, sortBy={}", page, size, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<User> userPage = userRepository.findAll(pageable);
        Page<UserResponse> responsePage = userPage.map(this::toResponse);
        return new PageResponse<>(responsePage);
    }

    public UserResponse getUserById(Long id) {
        log.info("Fetching user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        return toResponse(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });

        user.setName(request.getName());
        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("User updated successfully: {}", saved.getId());
        return toResponse(saved);
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        userRepository.delete(user);
        log.info("User deleted successfully: {}", id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStatus(),
                user.getTenant().getId(),
                user.getRole().getName()
        );
    }
}
