package com.saasforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasforge.dto.PageResponse;
import com.saasforge.dto.TenantRegistrationRequest;
import com.saasforge.dto.TenantResponse;
import com.saasforge.dto.UpdateTenantRequest;
import com.saasforge.exception.GlobalExceptionHandler;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TenantController tenantController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(tenantController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TenantRegistrationRequest validRequest() {
        TenantRegistrationRequest req = new TenantRegistrationRequest();
        req.setTenantName("Acme Corp");
        req.setAdminName("John Doe");
        req.setAdminEmail("john@acme.com");
        req.setPassword("securepassword");
        return req;
    }

    @Test
    void registerTenant_validRequest_returns200() throws Exception {
        doNothing().when(tenantService).registerTenant(any());

        mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(content().string("Tenant registered successfully"));

        verify(tenantService, times(1)).registerTenant(any());
    }

    @Test
    void registerTenant_missingTenantName_returns400() throws Exception {
        TenantRegistrationRequest req = validRequest();
        req.setTenantName("");

        mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerTenant_invalidEmail_returns400() throws Exception {
        TenantRegistrationRequest req = validRequest();
        req.setAdminEmail("not-an-email");

        mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerTenant_shortPassword_returns400() throws Exception {
        TenantRegistrationRequest req = validRequest();
        req.setPassword("short");

        mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    private TenantResponse sampleTenantResponse(Long id, String name) {
        return new TenantResponse(id, name, name.toLowerCase().replace(" ", "_"), "ACTIVE", null);
    }

    private PageResponse<TenantResponse> samplePage(TenantResponse... tenants) {
        return new PageResponse<>(new PageImpl<>(List.of(tenants), PageRequest.of(0, 10), tenants.length));
    }

    // ── GET /api/tenants ──────────────────────────────────────────────────────

    @Test
    void getAllTenants_returns200WithPagedContent() throws Exception {
        when(tenantService.getAllTenants(anyInt(), anyInt(), anyString())).thenReturn(samplePage(
                sampleTenantResponse(1L, "Acme Corp"),
                sampleTenantResponse(2L, "Beta Inc")
        ));

        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Acme Corp"))
                .andExpect(jsonPath("$.content[1].name").value("Beta Inc"));
    }

    @Test
    void getAllTenants_forwardsPageParams() throws Exception {
        when(tenantService.getAllTenants(eq(1), eq(5), eq("name"))).thenReturn(samplePage());

        mockMvc.perform(get("/api/tenants")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "name"))
                .andExpect(status().isOk());

        verify(tenantService).getAllTenants(1, 5, "name");
    }

    @Test
    void getAllTenants_returnsPageMetadata() throws Exception {
        when(tenantService.getAllTenants(anyInt(), anyInt(), anyString())).thenReturn(
                samplePage(sampleTenantResponse(1L, "Acme"))
        );

        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    // ── GET /api/tenants/{id} ─────────────────────────────────────────────────

    @Test
    void getTenantById_found_returns200() throws Exception {
        when(tenantService.getTenantById(1L)).thenReturn(sampleTenantResponse(1L, "Acme Corp"));

        mockMvc.perform(get("/api/tenants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getTenantById_notFound_returns404() throws Exception {
        when(tenantService.getTenantById(99L))
                .thenThrow(new ResourceNotFoundException("Tenant not found with id: 99"));

        mockMvc.perform(get("/api/tenants/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tenant not found with id: 99"));
    }

    // ── PUT /api/tenants/{id} ─────────────────────────────────────────────────

    @Test
    void updateTenant_validRequest_returns200() throws Exception {
        UpdateTenantRequest request = new UpdateTenantRequest();
        request.setName("Updated Corp");
        request.setStatus("INACTIVE");

        when(tenantService.updateTenant(eq(1L), any())).thenReturn(
                new TenantResponse(1L, "Updated Corp", "updated_corp", "INACTIVE", null)
        );

        mockMvc.perform(put("/api/tenants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Corp"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void updateTenant_blankName_returns400() throws Exception {
        UpdateTenantRequest request = new UpdateTenantRequest();
        request.setName("");
        request.setStatus("ACTIVE");

        mockMvc.perform(put("/api/tenants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTenant_blankStatus_returns400() throws Exception {
        UpdateTenantRequest request = new UpdateTenantRequest();
        request.setName("Acme");
        request.setStatus("");

        mockMvc.perform(put("/api/tenants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTenant_notFound_returns404() throws Exception {
        UpdateTenantRequest request = new UpdateTenantRequest();
        request.setName("Acme");
        request.setStatus("ACTIVE");

        when(tenantService.updateTenant(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Tenant not found with id: 99"));

        mockMvc.perform(put("/api/tenants/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tenant not found with id: 99"));
    }

    // ── DELETE /api/tenants/{id} ──────────────────────────────────────────────

    @Test
    void deleteTenant_success_returns204() throws Exception {
        doNothing().when(tenantService).deleteTenant(1L);

        mockMvc.perform(delete("/api/tenants/1"))
                .andExpect(status().isNoContent());

        verify(tenantService).deleteTenant(1L);
    }

    @Test
    void deleteTenant_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Tenant not found with id: 99"))
                .when(tenantService).deleteTenant(99L);

        mockMvc.perform(delete("/api/tenants/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/tenants/me ───────────────────────────────────────────────────

    @Test
    void getCurrentUser_returns200WithEmail() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("user@test.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/api/tenants/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged in as: user@test.com"));

        SecurityContextHolder.clearContext();
    }
}
