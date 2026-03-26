package com.saasforge.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void defaultValue_isNull() {
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }

    @Test
    void setAndGet_currentTenantId() {
        TenantContext.setCurrentTenantId(42L);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(42L);
    }

    @Test
    void overwrite_replacesExistingValue() {
        TenantContext.setCurrentTenantId(1L);
        TenantContext.setCurrentTenantId(99L);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(99L);
    }

    @Test
    void clear_removesCurrentTenantId() {
        TenantContext.setCurrentTenantId(42L);
        TenantContext.clear();
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }
}