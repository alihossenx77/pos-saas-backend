package com.pos.saas.payload.dto.analytics;

/** "active" stores are the APPROVED ones. */
public record SuperAdminSummaryDto(long totalStores, long activeStores, long pendingStores,
                                   long declinedStores, long blockedStores, long totalUsers) {}
