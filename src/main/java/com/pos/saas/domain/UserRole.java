package com.pos.saas.domain;

public enum UserRole {
    ROLE_USER,            // basic registered user (no store access)
    ROLE_SUPER_ADMIN,     // platform owner (manages all stores and plans)
    ROLE_STORE_ADMIN,     // owns exactly one store
    ROLE_BRANCH_MANAGER,
    ROLE_CASHIER
}
