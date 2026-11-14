package com.pos.saas.payload.dto.analytics;

/** type: OUT_OF_STOCK, LOW_STOCK, INACTIVE_EMPLOYEE, LONG_OPEN_SHIFT. severity: CRITICAL, WARNING, INFO. */
public record AlertDto(String type, String severity, String message, Long branchId, Long referenceId) {}
