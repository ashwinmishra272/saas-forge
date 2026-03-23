package com.saasforge.repository;

import com.saasforge.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantKey(String tenantKey);

    boolean existsByTenantKey(String tenantKey);

    boolean existsByName(String name);
}