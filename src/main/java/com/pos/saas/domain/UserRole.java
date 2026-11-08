package com.pos.saas.domain;

public enum UserRole {
    ROLE_SUPER_ADMIN,     // platform owner (manages all tenants)
    ROLE_STORE_ADMIN,     // tenant owner
    ROLE_BRANCH_MANAGER,
    ROLE_CASHIER
}
