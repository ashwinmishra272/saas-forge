package com.saasforge.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerTenant(TenantRegistrationRequest request) {

        if (tenantRepository.existsByName(request.getTenantName())) {
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
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TenantResponse getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));

        return toResponse(tenant);
    }

    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));

        tenant.setName(request.getName());
        tenant.setStatus(request.getStatus());

        Tenant saved = tenantRepository.save(tenant);
        return toResponse(saved);
    }

    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));

        tenantRepository.delete(tenant);
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
