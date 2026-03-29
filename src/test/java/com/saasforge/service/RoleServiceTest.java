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
import com.saasforge.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private RoleService roleService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Tenant buildTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        return tenant;
    }

    private SystemRole buildRole(Long id, String name, String roleKey) {
        SystemRole role = new SystemRole();
        role.setId(id);
        role.setName(name);
        role.setRoleKey(roleKey);
        role.setTenant(buildTenant());
        return role;
    }

    // ── getAllRoles ────────────────────────────────────────────────────────────

    @Test
    void getAllRoles_returnsRolesForCurrentTenant() {
        when(roleRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(
                buildRole(1L, "Admin", "ADMIN"),
                buildRole(2L, "Viewer", "VIEWER")
        ));

        List<RoleResponse> result = roleService.getAllRoles();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRoleKey()).isEqualTo("ADMIN");
        assertThat(result.get(1).getRoleKey()).isEqualTo("VIEWER");
    }

    @Test
    void getAllRoles_returnsEmptyList_whenNoRoles() {
        when(roleRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        List<RoleResponse> result = roleService.getAllRoles();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllRoles_mapsFieldsCorrectly() {
        when(roleRepository.findByTenantId(TENANT_ID)).thenReturn(
                List.of(buildRole(5L, "Manager", "MANAGER"))
        );

        List<RoleResponse> result = roleService.getAllRoles();

        RoleResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Manager");
        assertThat(response.getRoleKey()).isEqualTo("MANAGER");
        assertThat(response.getTenantId()).isEqualTo(TENANT_ID);
    }

    // ── getRoleById ───────────────────────────────────────────────────────────

    @Test
    void getRoleById_found_returnsRoleResponse() {
        when(roleRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(buildRole(1L, "Admin", "ADMIN")));

        RoleResponse result = roleService.getRoleById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Admin");
        assertThat(result.getRoleKey()).isEqualTo("ADMIN");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void getRoleById_notFound_throwsResourceNotFoundException() {
        when(roleRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createRole ────────────────────────────────────────────────────────────

    @Test
    void createRole_success_savesAndReturnsRole() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(roleRepository.existsByRoleKeyAndTenantId("EDITOR", TENANT_ID)).thenReturn(false);
        when(roleRepository.save(any(SystemRole.class))).thenAnswer(inv -> {
            SystemRole r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Editor");
        request.setRoleKey("EDITOR");

        RoleResponse result = roleService.createRole(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Editor");
        assertThat(result.getRoleKey()).isEqualTo("EDITOR");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createRole_setsTenantOnSavedEntity() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(roleRepository.existsByRoleKeyAndTenantId(anyString(), eq(TENANT_ID))).thenReturn(false);
        when(roleRepository.save(any(SystemRole.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Editor");
        request.setRoleKey("EDITOR");

        roleService.createRole(request);

        ArgumentCaptor<SystemRole> captor = ArgumentCaptor.forClass(SystemRole.class);
        verify(roleRepository).save(captor.capture());

        assertThat(captor.getValue().getTenant().getId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createRole_duplicateRoleKey_throwsBadRequestException() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(roleRepository.existsByRoleKeyAndTenantId("ADMIN", TENANT_ID)).thenReturn(true);

        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Admin");
        request.setRoleKey("ADMIN");

        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void createRole_tenantNotFound_throwsResourceNotFoundException() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Editor");
        request.setRoleKey("EDITOR");

        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ── updateRole ────────────────────────────────────────────────────────────

    @Test
    void updateRole_success_updatesNameAndReturns() {
        SystemRole existing = buildRole(1L, "OldName", "ADMIN");
        when(roleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(SystemRole.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("NewName");

        RoleResponse result = roleService.updateRole(1L, request);

        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getRoleKey()).isEqualTo("ADMIN");
    }

    @Test
    void updateRole_notFound_throwsResourceNotFoundException() {
        when(roleRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("NewName");

        assertThatThrownBy(() -> roleService.updateRole(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateRole_doesNotChangeRoleKey() {
        SystemRole existing = buildRole(1L, "OldName", "VIEWER");
        when(roleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(SystemRole.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");

        RoleResponse result = roleService.updateRole(1L, request);

        assertThat(result.getRoleKey()).isEqualTo("VIEWER");
    }

    // ── deleteRole ────────────────────────────────────────────────────────────

    @Test
    void deleteRole_success_softDeletesRole() {
        SystemRole role = buildRole(1L, "Admin", "ADMIN");
        when(roleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(role));

        roleService.deleteRole(1L);

        ArgumentCaptor<SystemRole> captor = ArgumentCaptor.forClass(SystemRole.class);
        verify(roleRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void deleteRole_notFound_throwsResourceNotFoundException() {
        when(roleRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.deleteRole(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteRole_notFound_doesNotSave() {
        when(roleRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.deleteRole(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(roleRepository, never()).save(any(SystemRole.class));
    }

    @Test
    void deleteRole_usesCurrentTenantIdFromContext() {
        SystemRole role = buildRole(1L, "Admin", "ADMIN");
        when(roleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(SystemRole.class))).thenReturn(role);

        roleService.deleteRole(1L);

        verify(roleRepository).findByIdAndTenantId(1L, TENANT_ID);
    }

    // ── tenant isolation ──────────────────────────────────────────────────────

    @Test
    void getAllRoles_usesCurrentTenantIdFromContext() {
        when(roleRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        roleService.getAllRoles();

        verify(roleRepository).findByTenantId(TENANT_ID);
        verify(roleRepository, never()).findByTenantId(argThat(id -> !id.equals(TENANT_ID)));
    }

    @Test
    void createRole_doesNotCallSave_whenDuplicateKeyExists() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(roleRepository.existsByRoleKeyAndTenantId("ADMIN", TENANT_ID)).thenReturn(true);

        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Admin");
        request.setRoleKey("ADMIN");

        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(BadRequestException.class);

        verify(roleRepository, never()).save(any(SystemRole.class));
    }

    @Test
    void updateRole_savesEntityWithUpdatedName() {
        SystemRole existing = buildRole(1L, "OldName", "ADMIN");
        when(roleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(SystemRole.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("NewName");

        roleService.updateRole(1L, request);

        ArgumentCaptor<SystemRole> captor = ArgumentCaptor.forClass(SystemRole.class);
        verify(roleRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("NewName");
        assertThat(captor.getValue().getRoleKey()).isEqualTo("ADMIN");
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }
}