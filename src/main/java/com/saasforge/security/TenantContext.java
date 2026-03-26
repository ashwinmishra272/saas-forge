package com.saasforge.security;

public class TenantContext {

    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    public static void setCurrentTenantId(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    public static Long getCurrentTenantId() {
        return currentTenantId.get();
    }

    public static void clear() {
        currentTenantId.remove();
    }
}
