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
import com.saasforge.security.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
        tenantRepository.save(tenant);

        SystemRole adminRole = new SystemRole();
        adminRole.setName("ADMIN");
        adminRole.setRoleKey("ADMIN");
        adminRole.setTenant(tenant);
        roleRepository.save(adminRole);

        User adminUser = new User();
        adminUser.setName(request.getAdminName());
        adminUser.setEmail(request.getAdminEmail());
        adminUser.setPassword(passwordEncoder.encode(request.getPassword()));
        adminUser.setTenant(tenant);
        adminUser.setRole(adminRole);
        adminUser.setStatus("ACTIVE");
        userRepository.save(adminUser);

        log.info("Tenant registered successfully: {} (id={})", tenant.getName(), tenant.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "tenants", key = "T(com.saasforge.security.TenantContext).getCurrentTenantId()")
    public PageResponse<TenantResponse> getAllTenants(int page, int size, String sortBy, String search) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Fetching tenants for tenantId={} page={} size={} search={}", tenantId, page, size, search);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Tenant> tenantPage = (search == null || search.isBlank())
                ? tenantRepository.findByIdAndDeletedFalse(tenantId, pageable)
                : tenantRepository.searchById(tenantId, search.trim(), pageable);

        return new PageResponse<>(tenantPage.map(this::toResponse));
    }


    @PreAuthorize("hasRole('ADMIN')")
    public TenantResponse getTenantById(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Fetching tenant id={} for tenantId={}", id, tenantId);
        // Users can only fetch their own tenant — ignore id param, use JWT tenantId
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> {
                    log.warn("Tenant not found with id: {}", tenantId);
                    return new ResourceNotFoundException("Tenant not found with id: " + tenantId);
                });
        return toResponse(tenant);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "tenants", key = "T(com.saasforge.security.TenantContext).getCurrentTenantId()")
    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating tenant id={} for tenantId={}", id, tenantId);
        // Scope update to current tenant only
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> {
                    log.warn("Tenant not found with id: {}", tenantId);
                    return new ResourceNotFoundException("Tenant not found with id: " + tenantId);
                });

        tenant.setName(request.getName());
        tenant.setStatus(request.getStatus());

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant updated successfully: {}", saved.getId());
        return toResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "tenants", key = "T(com.saasforge.security.TenantContext).getCurrentTenantId()")
    public void deleteTenant(Long id) {
        log.info("Soft deleting tenant id={}", id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tenant not found id={}", id);
                    return new ResourceNotFoundException("Tenant not found with id: " + id);
                });

        tenant.setDeleted(true);
        tenant.setDeletedAt(LocalDateTime.now());
        tenantRepository.save(tenant);

        log.info("Tenant soft deleted id={}", id);
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
