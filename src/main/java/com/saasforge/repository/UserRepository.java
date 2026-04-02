package com.saasforge.repository;

import com.saasforge.entity.User;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@SQLRestriction("deleted = false")
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Page<User> findByTenantId(Long tenantId, Pageable pageable);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByEmailAndTenantId(String email, Long tenantId);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId " +
            "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchByTenantId(@Param("tenantId") Long tenantId,
                                @Param("search") String search,
                                Pageable pageable);

}
