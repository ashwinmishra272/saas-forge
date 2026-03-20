package com.saasforge.repository;

import com.saasforge.entity.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<SystemRole, Long> {
}