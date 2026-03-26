package com.saasforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasforge.dto.CreateRoleRequest;
import com.saasforge.dto.RoleResponse;
import com.saasforge.dto.UpdateRoleRequest;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.GlobalExceptionHandler;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RoleResponse sampleRole(Long id, String name, String roleKey) {
        return new RoleResponse(id, name, roleKey, 1L);
    }

    // ── GET /api/roles ────────────────────────────────────────────────────────

    @Test
    void getAllRoles_returns200WithList() throws Exception {
        when(roleService.getAllRoles()).thenReturn(List.of(
                sampleRole(1L, "Admin", "ADMIN"),
                sampleRole(2L, "Viewer", "VIEWER")
        ));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].roleKey").value("ADMIN"))
                .andExpect(jsonPath("$[1].roleKey").value("VIEWER"));
    }

    @Test
    void getAllRoles_returnsEmptyList() throws Exception {
        when(roleService.getAllRoles()).thenReturn(List.of());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllRoles_mapsAllFieldsInListItems() throws Exception {
        when(roleService.getAllRoles()).thenReturn(List.of(sampleRole(7L, "Manager", "MANAGER")));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Manager"))
                .andExpect(jsonPath("$[0].roleKey").value("MANAGER"))
                .andExpect(jsonPath("$[0].tenantId").value(1));
    }

    // ── GET /api/roles/{id} ───────────────────────────────────────────────────

    @Test
    void getRoleById_found_returns200() throws Exception {
        when(roleService.getRoleById(1L)).thenReturn(sampleRole(1L, "Admin", "ADMIN"));

        mockMvc.perform(get("/api/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Admin"))
                .andExpect(jsonPath("$.roleKey").value("ADMIN"))
                .andExpect(jsonPath("$.tenantId").value(1));
    }

    @Test
    void getRoleById_notFound_returns404() throws Exception {
        when(roleService.getRoleById(99L)).thenThrow(new ResourceNotFoundException("Role not found with id: 99"));

        mockMvc.perform(get("/api/roles/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Role not found with id: 99"));
    }

    // ── POST /api/roles ───────────────────────────────────────────────────────

    @Test
    void createRole_validRequest_returns201() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Editor");
        request.setRoleKey("EDITOR");

        when(roleService.createRole(any())).thenReturn(sampleRole(10L, "Editor", "EDITOR"));

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.roleKey").value("EDITOR"));
    }

    @Test
    void createRole_blankName_returns400() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("");
        request.setRoleKey("EDITOR");

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_nullName_returns400() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName(null);
        request.setRoleKey("EDITOR");

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_blankRoleKey_returns400() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Editor");
        request.setRoleKey("");

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_nullRoleKey_returns400() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Editor");
        request.setRoleKey(null);

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_tenantNotFound_returns404() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Editor");
        request.setRoleKey("EDITOR");

        when(roleService.createRole(any())).thenThrow(new ResourceNotFoundException("Tenant not found"));

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tenant not found"));
    }

    @Test
    void createRole_invalidRoleKeyPattern_returns400() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Editor");
        request.setRoleKey("editor_role"); // lowercase — violates ^[A-Z_]+$

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_duplicateKey_returns400() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Admin");
        request.setRoleKey("ADMIN");

        when(roleService.createRole(any())).thenThrow(new BadRequestException("Role with key 'ADMIN' already exists"));

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role with key 'ADMIN' already exists"));
    }

    // ── PUT /api/roles/{id} ───────────────────────────────────────────────────

    @Test
    void updateRole_validRequest_returns200() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated Admin");

        when(roleService.updateRole(eq(1L), any())).thenReturn(sampleRole(1L, "Updated Admin", "ADMIN"));

        mockMvc.perform(put("/api/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Admin"));
    }

    @Test
    void updateRole_blankName_returns400() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("");

        mockMvc.perform(put("/api/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRole_nullName_returns400() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName(null);

        mockMvc.perform(put("/api/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRole_passesCorrectIdToService() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");

        when(roleService.updateRole(eq(5L), any())).thenReturn(sampleRole(5L, "Updated", "VIEWER"));

        mockMvc.perform(put("/api/roles/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(roleService).updateRole(eq(5L), any());
    }

    @Test
    void updateRole_notFound_returns404() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");

        when(roleService.updateRole(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Role not found with id: 99"));

        mockMvc.perform(put("/api/roles/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Role not found with id: 99"));
    }

    // ── DELETE /api/roles/{id} ────────────────────────────────────────────────

    @Test
    void deleteRole_success_returns204() throws Exception {
        doNothing().when(roleService).deleteRole(1L);

        mockMvc.perform(delete("/api/roles/1"))
                .andExpect(status().isNoContent());

        verify(roleService).deleteRole(1L);
    }

    @Test
    void deleteRole_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Role not found with id: 99"))
                .when(roleService).deleteRole(99L);

        mockMvc.perform(delete("/api/roles/99"))
                .andExpect(status().isNotFound());
    }
}