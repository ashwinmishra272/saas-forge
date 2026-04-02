package com.saasforge.repository;

import com.saasforge.entity.SystemRole;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

@SQLRestriction("deleted = false")
public interface RoleRepository extends JpaRepository<SystemRole, Long> {

    List<SystemRole> findByTenantId(Long tenantId);

    Optional<SystemRole> findByIdAndTenantId(Long id, Long tenantId);

    Optional<SystemRole> findByRoleKeyAndTenantId(String roleKey, Long tenantId);

    boolean existsByRoleKeyAndTenantId(String roleKey, Long tenantId);

    List<SystemRole> findByTenantIdAndNameContainingIgnoreCase(Long tenantId, String name);

}
