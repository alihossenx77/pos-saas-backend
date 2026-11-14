package com.pos.saas.payload.dto.analytics;

import java.math.BigDecimal;

/** monthGrowthPercent = month-to-date vs the same number of days of last month (null if none). */
public record StoreOverviewDto(Long storeId, String brand,
                               PeriodStatsDto today, PeriodStatsDto yesterday,
                               PeriodStatsDto monthToDate, PeriodStatsDto lastMonthSamePeriod,
                               BigDecimal monthGrowthPercent,
                               long branches, long employees, long products, long customers, long openShifts) {}
