package com.saasforge.controller;

import com.saasforge.dto.TenantRegistrationRequest;
import com.saasforge.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    public ResponseEntity<String> registerTenant(
            @Valid @RequestBody TenantRegistrationRequest request) {

        tenantService.registerTenant(request);
        return ResponseEntity.ok("Tenant registered successfully");
    }
}
