package com.saasforge.repository;

import com.saasforge.entity.Tenant;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@SQLRestriction("deleted = false")
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantKey(String tenantKey);

    boolean existsByTenantKey(String tenantKey);

    boolean existsByName(String name);

    // Used by tenant admins — scoped to their own workspace only
    Page<Tenant> findByIdAndDeletedFalse(Long id, Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE t.id = :tenantId " +
            "AND LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Tenant> searchById(@Param("tenantId") Long tenantId,
                            @Param("search") String search,
                            Pageable pageable);

}
