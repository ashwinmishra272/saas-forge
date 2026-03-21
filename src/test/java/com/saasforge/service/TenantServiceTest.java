package com.saasforge.service;

import com.saasforge.dto.TenantRegistrationRequest;
import com.saasforge.entity.SystemRole;
import com.saasforge.entity.Tenant;
import com.saasforge.entity.User;
import com.saasforge.repository.RoleRepository;
import com.saasforge.repository.TenantRepository;
import com.saasforge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantService tenantService;

    private TenantRegistrationRequest buildRequest() {
        TenantRegistrationRequest request = new TenantRegistrationRequest();
        request.setTenantName("Acme Corp");
        request.setAdminName("John Doe");
        request.setAdminEmail("john@acme.com");
        request.setPassword("securepassword");
        return request;
    }

    @Test
    void registerTenant_savesTenantWithCorrectFields() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        tenantService.registerTenant(buildRequest());

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());

        Tenant saved = tenantCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Acme Corp");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void registerTenant_tenantKey_isLowercaseWithUnderscores() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        tenantService.registerTenant(buildRequest());

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());

        assertThat(tenantCaptor.getValue().getTenantKey()).isEqualTo("acme_corp");
    }

    @Test
    void registerTenant_savesAdminRoleWithCorrectFields() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        tenantService.registerTenant(buildRequest());

        ArgumentCaptor<SystemRole> roleCaptor = ArgumentCaptor.forClass(SystemRole.class);
        verify(roleRepository).save(roleCaptor.capture());

        SystemRole role = roleCaptor.getValue();
        assertThat(role.getName()).isEqualTo("ADMIN");
        assertThat(role.getRoleKey()).isEqualTo("ADMIN");
        assertThat(role.getTenant()).isNotNull();
    }

    @Test
    void registerTenant_savesAdminUserWithEncodedPassword() {
        when(passwordEncoder.encode("securepassword")).thenReturn("encodedPass");

        tenantService.registerTenant(buildRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User user = userCaptor.getValue();
        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@acme.com");
        assertThat(user.getPassword()).isEqualTo("encodedPass");
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void registerTenant_callsSaveOnAllThreeRepositories() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        tenantService.registerTenant(buildRequest());

        verify(tenantRepository, times(1)).save(any(Tenant.class));
        verify(roleRepository, times(1)).save(any(SystemRole.class));
        verify(userRepository, times(1)).save(any(User.class));
    }
}
