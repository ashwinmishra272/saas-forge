package com.saasforge.service;

import com.saasforge.dto.PageResponse;
import com.saasforge.dto.TenantRegistrationRequest;
import com.saasforge.dto.TenantResponse;
import com.saasforge.dto.UpdateTenantRequest;
import com.saasforge.entity.SystemRole;
import com.saasforge.entity.Tenant;
import com.saasforge.entity.User;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.RoleRepository;
import com.saasforge.repository.TenantRepository;
import com.saasforge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void registerTenant_duplicateName_throwsBadRequestException() {
        when(tenantRepository.existsByName("Acme Corp")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.registerTenant(buildRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Acme Corp");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    // ── getAllTenants ──────────────────────────────────────────────────────────

    @Test
    void getAllTenants_returnsPaginatedResponse() {
        Tenant t1 = buildTenant(1L, "Acme", "acme", "ACTIVE");
        Tenant t2 = buildTenant(2L, "Beta Corp", "beta_corp", "ACTIVE");
        Page<Tenant> page = new PageImpl<>(List.of(t1, t2));
        when(tenantRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<TenantResponse> result = tenantService.getAllTenants(0, 10, "id");

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Acme");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Beta Corp");
    }

    @Test
    void getAllTenants_passesCorrectPageableToRepository() {
        when(tenantRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        tenantService.getAllTenants(2, 5, "name");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tenantRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void getAllTenants_mapsFieldsCorrectly() {
        Tenant t = buildTenant(7L, "Demo Inc", "demo_inc", "ACTIVE");
        when(tenantRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(t)));

        PageResponse<TenantResponse> result = tenantService.getAllTenants(0, 10, "id");

        TenantResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getName()).isEqualTo("Demo Inc");
        assertThat(response.getTenantKey()).isEqualTo("demo_inc");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    // ── getTenantById ─────────────────────────────────────────────────────────

    @Test
    void getTenantById_found_returnsTenantResponse() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(buildTenant(1L, "Acme", "acme", "ACTIVE")));

        TenantResponse result = tenantService.getTenantById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Acme");
        assertThat(result.getTenantKey()).isEqualTo("acme");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getTenantById_notFound_throwsResourceNotFoundException() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── updateTenant ──────────────────────────────────────────────────────────

    @Test
    void updateTenant_success_updatesNameAndStatus() {
        Tenant existing = buildTenant(1L, "OldName", "old_name", "ACTIVE");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTenantRequest request = new UpdateTenantRequest();
        request.setName("NewName");
        request.setStatus("INACTIVE");

        TenantResponse result = tenantService.updateTenant(1L, request);

        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void updateTenant_notFound_throwsResourceNotFoundException() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateTenantRequest request = new UpdateTenantRequest();
        request.setName("Name");
        request.setStatus("ACTIVE");

        assertThatThrownBy(() -> tenantService.updateTenant(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── deleteTenant ──────────────────────────────────────────────────────────

    @Test
    void deleteTenant_success_callsDelete() {
        Tenant tenant = buildTenant(1L, "Acme", "acme", "ACTIVE");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        tenantService.deleteTenant(1L);

        verify(tenantRepository).delete(tenant);
    }

    @Test
    void deleteTenant_notFound_throwsResourceNotFoundException() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.deleteTenant(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteTenant_notFound_doesNotCallDelete() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.deleteTenant(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tenantRepository, never()).delete(any(Tenant.class));
    }

    private Tenant buildTenant(Long id, String name, String key, String status) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setTenantKey(key);
        tenant.setStatus(status);
        tenant.setCreatedAt(LocalDateTime.now());
        return tenant;
    }
}
