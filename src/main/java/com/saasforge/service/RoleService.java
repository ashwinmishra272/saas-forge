package com.saasforge.service;

import com.saasforge.dto.CreateRoleRequest;
import com.saasforge.dto.RoleResponse;
import com.saasforge.dto.UpdateRoleRequest;
import com.saasforge.entity.SystemRole;
import com.saasforge.entity.Tenant;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.RoleRepository;
import com.saasforge.repository.TenantRepository;
import com.saasforge.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public List<RoleResponse> getAllRoles() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Fetching all roles for tenantId={}", tenantId);

        return roleRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse getRoleById(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();

        SystemRole role = roleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        return toResponse(role);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse createRole(CreateRoleRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating role: {} for tenantId={}", request.getRoleKey(), tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (roleRepository.existsByRoleKeyAndTenantId(request.getRoleKey(), tenantId)) {
            throw new BadRequestException("Role with key '" + request.getRoleKey() + "' already exists");
        }

        SystemRole role = new SystemRole();
        role.setName(request.getName());
        role.setRoleKey(request.getRoleKey());
        role.setTenant(tenant);

        SystemRole saved = roleRepository.save(role);
        log.info("Role created id={}", saved.getId());
        return toResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();

        SystemRole role = roleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        role.setName(request.getName());
        SystemRole saved = roleRepository.save(role);
        log.info("Role updated id={}", saved.getId());
        return toResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRole(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Soft deleting role id={} tenantId={}", id, tenantId);

        SystemRole role = roleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        role.setDeleted(true);
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);

        log.info("Role soft deleted id={}", id);
    }

    private RoleResponse toResponse(SystemRole role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getRoleKey(),
                role.getTenant().getId()
        );
    }
}
