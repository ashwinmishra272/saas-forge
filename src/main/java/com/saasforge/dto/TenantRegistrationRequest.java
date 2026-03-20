package com.saasforge.dto;

import lombok.Data;

@Data
public class TenantRegistrationRequest {

    public String getUserName;
    private String tenantName;
    private String adminName;
    private String adminEmail;
    private String password;

}