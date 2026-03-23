package com.saasforge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoleResponse {

    private Long id;
    private String name;
    private String roleKey;
    private Long tenantId;
}
