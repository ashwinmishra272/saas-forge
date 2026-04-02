package com.saasforge.controller;

import com.saasforge.dto.PageResponse;
import com.saasforge.dto.TenantRegistrationRequest;
import com.saasforge.dto.TenantResponse;
import com.saasforge.dto.UpdateTenantRequest;
import com.saasforge.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant management")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    @Operation(summary = "Register tenant", description = "Register a new tenant with an admin user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant registered successfully"),
            @ApiResponse(responseCode = "400", description = "Tenant name already exists or validation failed")
    })
    public ResponseEntity<String> registerTenant(
            @Valid @RequestBody TenantRegistrationRequest request) {
        tenantService.registerTenant(request);
        return ResponseEntity.ok("Tenant registered successfully");
    }

    @GetMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get all tenants", description = "Returns paginated list of all tenants (ADMIN only)")
    public ResponseEntity<PageResponse<TenantResponse>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(tenantService.getAllTenants(page, size, sortBy, search));
    }


    @GetMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get tenant by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant found"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantResponse> getTenantById(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get current user", description = "Returns the email of the currently authenticated user")
    public ResponseEntity<String> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok("Logged in as: " + email);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update tenant", description = "Update tenant name and status (ADMIN only)")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete tenant", description = "Permanently delete a tenant (ADMIN only)")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }
}
