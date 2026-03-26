package com.saasforge.service;

import com.saasforge.dto.PageResponse;
import com.saasforge.dto.TenantRegistrationRequest;
import com.saasforge.dto.TenantResponse;
import com.saasforge.dto.UpdateTenantRequest;
import com.saasforge.entity.SystemRole;
import com.saasforge.entity.Tenant;
import com.saasforge.entity.User;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.RoleRepository;
import com.saasforge.repository.TenantRepository;
import com.saasforge.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerTenant(TenantRegistrationRequest request) {
        log.info("Registering new tenant: {}", request.getTenantName());

        if (tenantRepository.existsByName(request.getTenantName())) {
            log.warn("Tenant registration failed - name already exists: {}", request.getTenantName());
            throw new BadRequestException("Tenant with name '" + request.getTenantName() + "' already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.getTenantName());
        tenant.setTenantKey(request.getTenantName().toLowerCase().replace(" ", "_"));
        tenant.setStatus("ACTIVE");
        tenant.setCreatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);

        SystemRole adminRole = new SystemRole();
        adminRole.setName("ADMIN");
        adminRole.setRoleKey("ADMIN");
        adminRole.setTenant(tenant);
        adminRole.setCreatedAt(LocalDateTime.now());
        roleRepository.save(adminRole);

        User adminUser = new User();
        adminUser.setName(request.getAdminName());
        adminUser.setEmail(request.getAdminEmail());
        adminUser.setPassword(passwordEncoder.encode(request.getPassword()));
        adminUser.setTenant(tenant);
        adminUser.setRole(adminRole);
        adminUser.setStatus("ACTIVE");
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(adminUser);

        log.info("Tenant registered successfully: {} (id={})", tenant.getName(), tenant.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<TenantResponse> getAllTenants(int page, int size, String sortBy) {
        log.info("Fetching tenants - page={}, size={}, sortBy={}", page, size, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Tenant> tenantPage = tenantRepository.findAll(pageable);
        Page<TenantResponse> responsePage = tenantPage.map(this::toResponse);
        return new PageResponse<>(responsePage);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public TenantResponse getTenantById(Long id) {
        log.info("Fetching tenant with id: {}", id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tenant not found with id: {}", id);
                    return new ResourceNotFoundException("Tenant not found with id: " + id);
                });
        return toResponse(tenant);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        log.info("Updating tenant with id: {}", id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tenant not found with id: {}", id);
                    return new ResourceNotFoundException("Tenant not found with id: " + id);
                });

        tenant.setName(request.getName());
        tenant.setStatus(request.getStatus());

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant updated successfully: {}", saved.getId());
        return toResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTenant(Long id) {
        log.info("Deleting tenant with id: {}", id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tenant not found with id: {}", id);
                    return new ResourceNotFoundException("Tenant not found with id: " + id);
                });
        tenantRepository.delete(tenant);
        log.info("Tenant deleted successfully: {}", id);
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getTenantKey(),
                tenant.getStatus(),
                tenant.getCreatedAt()
        );
    }
}
