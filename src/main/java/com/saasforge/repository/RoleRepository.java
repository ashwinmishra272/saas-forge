package com.saasforge.repository;

import com.saasforge.entity.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<SystemRole, Long> {

    List<SystemRole> findByTenantId(Long tenantId);

    Optional<SystemRole> findByRoleKeyAndTenantId(String roleKey, Long tenantId);

    boolean existsByRoleKeyAndTenantId(String roleKey, Long tenantId);
}
