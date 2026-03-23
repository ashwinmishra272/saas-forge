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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoleResponse getRoleById(Long id) {
        SystemRole role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        return toResponse(role);
    }

    public RoleResponse createRole(CreateRoleRequest request) {

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + request.getTenantId()));

        boolean roleKeyExists = roleRepository.existsByRoleKeyAndTenantId(
                request.getRoleKey(), request.getTenantId());

        if (roleKeyExists) {
            throw new BadRequestException("Role with key '" + request.getRoleKey() + "' already exists in this tenant");
        }

        SystemRole role = new SystemRole();
        role.setName(request.getName());
        role.setRoleKey(request.getRoleKey());
        role.setTenant(tenant);
        role.setCreatedAt(LocalDateTime.now());

        SystemRole saved = roleRepository.save(role);
        return toResponse(saved);
    }

    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        SystemRole role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        role.setName(request.getName());

        SystemRole saved = roleRepository.save(role);
        return toResponse(saved);
    }

    public void deleteRole(Long id) {
        SystemRole role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        roleRepository.delete(role);
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
