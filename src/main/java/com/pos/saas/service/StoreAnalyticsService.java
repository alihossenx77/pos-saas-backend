package com.pos.saas.service;

import com.pos.saas.model.Store;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.analytics.*;
import com.pos.saas.service.AnalyticsCalculator.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** The path variable {storeAdminId} must be the logged-in store admin (anything else is a 404). */
@Service
@RequiredArgsConstructor
public class StoreAnalyticsService {

    private final AnalyticsCalculator calc;
    private final AccessService access;

    public StoreOverviewDto overview(Long storeAdminId) {
        Store store = store(storeAdminId);
        Long id = store.getId();

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        // same number of days as the current month has elapsed, never running into this month
        LocalDate lastMonthEnd = lastMonthStart.plusDays(today.getDayOfMonth());
        if (lastMonthEnd.isAfter(monthStart)) lastMonthEnd = monthStart;

        PeriodStatsDto todayStats = calc.stats(id, null, Window.day(today));
        PeriodStatsDto yesterday = calc.stats(id, null, Window.day(today.minusDays(1)));
        PeriodStatsDto mtd = calc.stats(id, null, new Window(monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        PeriodStatsDto lastMtd = calc.stats(id, null, new Window(lastMonthStart.atStartOfDay(), lastMonthEnd.atStartOfDay()));

        return new StoreOverviewDto(id, store.getBrand(), todayStats, yesterday, mtd, lastMtd,
                calc.growthPercent(mtd.grossSales(), lastMtd.grossSales()),
                calc.branches(id), calc.employees(id), calc.products(id), calc.customers(id), calc.openShifts(id, null));
    }

    /** period = daily (last 30 days) | weekly (last 12 weeks) | monthly (last 12 months) */
    public List<SalesPointDto> trends(Long storeAdminId, String period) {
        Long id = store(storeAdminId).getId();
        return switch (period == null ? "daily" : period.toLowerCase()) {
            case "daily" -> calc.daily(id, null, 30);
            case "weekly" -> calc.weekly(id, null, 12);
            case "monthly" -> calc.monthly(id, null, 12);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "period must be daily, weekly or monthly");
        };
    }

    public List<SalesPointDto> daily(Long storeAdminId, int days) {
        return calc.daily(store(storeAdminId).getId(), null, AnalyticsCalculator.bounded(days, 1, 90, "days"));
    }

    public List<SalesPointDto> monthly(Long storeAdminId, int months) {
        return calc.monthly(store(storeAdminId).getId(), null, AnalyticsCalculator.bounded(months, 1, 24, "months"));
    }

    public List<CategorySalesDto> categories(Long storeAdminId, int days) {
        return calc.categorySales(store(storeAdminId).getId(), null, window(days));
    }

    public List<PaymentBreakdownDto> payments(Long storeAdminId, int days) {
        return calc.paymentBreakdown(store(storeAdminId).getId(), null, window(days));
    }

    public List<BranchPerformanceDto> branches(Long storeAdminId, int days) {
        return calc.branchPerformance(store(storeAdminId).getId(), window(days));
    }

    public List<AlertDto> alerts(Long storeAdminId) {
        return calc.alerts(store(storeAdminId).getId(), null);
    }

    private Window window(int days) {
        return Window.lastDays(AnalyticsCalculator.bounded(days, 1, 365, "days"));
    }

    private Store store(Long storeAdminId) {
        User admin = access.currentAdmin();
        if (!admin.getId().equals(storeAdminId)) throw AccessService.notFound("Store not found");
        return access.myStore();
    }
}
