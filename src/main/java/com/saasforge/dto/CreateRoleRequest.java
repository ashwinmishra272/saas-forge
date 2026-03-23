package com.saasforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    @NotBlank(message = "Role key is required")
    @Pattern(regexp = "^[A-Z_]+$", message = "Role key must be uppercase letters and underscores only")
    private String roleKey;

    private Long tenantId;
}
