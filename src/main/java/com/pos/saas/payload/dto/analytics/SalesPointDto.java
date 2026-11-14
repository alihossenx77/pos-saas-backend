package com.pos.saas.payload.dto.analytics;

import java.math.BigDecimal;

/** One point of a sales chart. label = "2026-11-10" (day), "2026-11-09" (week start, Monday) or "2026-11" (month). */
public record SalesPointDto(String label, long orders, BigDecimal grossSales, BigDecimal refunds, BigDecimal netSales) {}
