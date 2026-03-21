package com.saasforge.dto;

import lombok.Data;

@Data
public class TenantRegistrationRequest {

    private String userName;
    private String tenantName;
    private String adminName;
    private String adminEmail;
    private String password;

}