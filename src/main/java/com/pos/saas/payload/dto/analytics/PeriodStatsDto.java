package com.pos.saas.payload.dto.analytics;

import java.math.BigDecimal;

public record PeriodStatsDto(long orders, BigDecimal grossSales, BigDecimal refunds, BigDecimal netSales,
                             BigDecimal avgOrderValue, long uniqueCustomers) {
}
