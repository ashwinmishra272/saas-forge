package com.saasforge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TenantResponse {

    private Long id;
    private String name;
    private String tenantKey;
    private String status;
    private LocalDateTime createdAt;
}
