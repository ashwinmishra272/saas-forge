package com.saasforge.service;

import com.saasforge.dto.PageResponse;
import com.saasforge.dto.UpdateUserRequest;
import com.saasforge.dto.UserResponse;
import com.saasforge.entity.User;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.UserRepository;
import com.saasforge.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> getAllUsers(int page, int size, String sortBy) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Fetching users for tenantId={} page={} size={}", tenantId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<User> userPage = userRepository.findByTenantId(tenantId, pageable);
        return new PageResponse<>(userPage.map(this::toResponse));
    }

    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public UserResponse getUserById(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Fetching user id={} tenantId={}", id, tenantId);

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return toResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating user id={} tenantId={}", id, tenantId);

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setName(request.getName());
        user.setStatus(request.getStatus());

        User saved = userRepository.save(user);
        log.info("User updated id={}", saved.getId());
        return toResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Deleting user id={} tenantId={}", id, tenantId);

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.delete(user);
        log.info("User deleted id={}", id);
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
