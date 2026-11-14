package com.pos.saas.payload.dto.analytics;

import java.math.BigDecimal;

/** salesGrowthPercent = today vs the whole of yesterday (null when yesterday had no sales). */
public record BranchOverviewDto(Long branchId, String branchName, PeriodStatsDto today, PeriodStatsDto yesterday,
                                BigDecimal salesGrowthPercent, long openShifts) {}
