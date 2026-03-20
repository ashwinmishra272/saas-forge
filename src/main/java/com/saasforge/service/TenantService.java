package com.saasforge.service;

import com.saasforge.dto.TenantRegistrationRequest;
import com.saasforge.entity.SystemRole;
import com.saasforge.entity.Tenant;
import com.saasforge.entity.User;
import com.saasforge.repository.RoleRepository;
import com.saasforge.repository.TenantRepository;
import com.saasforge.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(
            TenantRepository tenantRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerTenant(TenantRegistrationRequest request) {

        // 1 Create tenant
        Tenant tenant = new Tenant();

        tenant.setName(request.getTenantName());   // THIS WAS MISSING
        tenant.setTenantKey(request.getTenantName().toLowerCase().replace(" ", "_"));
        tenant.setStatus("ACTIVE");
        tenant.setCreatedAt(LocalDateTime.now());

        tenantRepository.save(tenant);

        // 2 Create admin role
        SystemRole adminRole = new SystemRole();
        adminRole.setName("ADMIN");
        adminRole.setRoleKey("ADMIN");
        adminRole.setTenant(tenant);
        roleRepository.save(adminRole);

        // 3 Create admin user
        User adminUser = new User();
        adminUser.setName(request.getAdminName());
        adminUser.setEmail(request.getAdminEmail());
        adminUser.setPassword(passwordEncoder.encode(request.getPassword()));
        adminUser.setTenant(tenant);
        adminUser.setRole(adminRole);

        userRepository.save(adminUser);
    }
}