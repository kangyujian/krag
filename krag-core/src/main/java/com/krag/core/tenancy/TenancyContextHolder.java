package com.krag.core.tenancy;

public interface TenancyContextHolder {
    void set(String tenantId, String kbId);
    String tenantId();
    String kbId();
    void clear();
}