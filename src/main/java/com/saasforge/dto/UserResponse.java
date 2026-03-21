package com.saasforge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private Long tenantId;
    private String name;
    private String email;
    private String status;
    private String roleName;
}
