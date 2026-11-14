package com.pos.saas.payload.dto.analytics;

import java.math.BigDecimal;

public record BranchPerformanceDto(Long branchId, String branchName, long orders, BigDecimal grossSales,
                                   BigDecimal refunds, BigDecimal netSales, BigDecimal avgOrderValue) {}
