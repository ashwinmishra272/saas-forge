package com.saasforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasforge.dto.TenantRegistrationRequest;
import com.saasforge.exception.GlobalExceptionHandler;
import com.saasforge.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
