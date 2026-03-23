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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;

    public List<RoleResponse> getAllRoles() {
        log.info("Fetching all roles");
        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoleResponse getRoleById(Long id) {
        log.info("Fetching role with id: {}", id);
        SystemRole role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Role not found with id: {}", id);
                    return new ResourceNotFoundException("Role not found with id: " + id);
                });
        return toResponse(role);
    }

    public RoleResponse createRole(CreateRoleRequest request) {
        log.info("Creating role: {} for tenantId: {}", request.getRoleKey(), request.getTenantId());

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> {
                    log.warn("Tenant not found with id: {}", request.getTenantId());
                    return new ResourceNotFoundException("Tenant not found with id: " + request.getTenantId());
                });

        if (roleRepository.existsByRoleKeyAndTenantId(request.getRoleKey(), request.getTenantId())) {
            log.warn("Role creation failed - key already exists: {} in tenant: {}", request.getRoleKey(), request.getTenantId());
            throw new BadRequestException("Role with key '" + request.getRoleKey() + "' already exists in this tenant");
        }

        SystemRole role = new SystemRole();
        role.setName(request.getName());
        role.setRoleKey(request.getRoleKey());
        role.setTenant(tenant);
        role.setCreatedAt(LocalDateTime.now());

        SystemRole saved = roleRepository.save(role);
        log.info("Role created successfully: {} (id={})", saved.getRoleKey(), saved.getId());
        return toResponse(saved);
    }

    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        log.info("Updating role with id: {}", id);
        SystemRole role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Role not found with id: {}", id);
                    return new ResourceNotFoundException("Role not found with id: " + id);
                });

        role.setName(request.getName());

        SystemRole saved = roleRepository.save(role);
        log.info("Role updated successfully: {}", saved.getId());
        return toResponse(saved);
    }

    public void deleteRole(Long id) {
        log.info("Deleting role with id: {}", id);
        SystemRole role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Role not found with id: {}", id);
                    return new ResourceNotFoundException("Role not found with id: " + id);
                });
        roleRepository.delete(role);
        log.info("Role deleted successfully: {}", id);
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
