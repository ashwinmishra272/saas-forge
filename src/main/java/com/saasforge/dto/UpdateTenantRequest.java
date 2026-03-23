package com.saasforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTenantRequest {

    @NotBlank(message = "Tenant name is required")
    private String name;

    @NotBlank(message = "Status is required")
    private String status;
}
