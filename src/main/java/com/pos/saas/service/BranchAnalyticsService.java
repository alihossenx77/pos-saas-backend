package com.pos.saas.service;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.Branch;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.analytics.*;
import com.pos.saas.service.AnalyticsCalculator.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Store admin: any branch of the store. Branch manager: own branch (branchId may be left out). */
@Service
@RequiredArgsConstructor
public class BranchAnalyticsService {

    private final AnalyticsCalculator calc;
    private final AccessService access;

    public List<SalesPointDto> dailySales(Long branchId, int days) {
        Branch b = branch(branchId);
        return calc.daily(b.getStoreId(), b.getId(), AnalyticsCalculator.bounded(days, 1, 90, "days"));
    }

    public List<TopProductStatDto> topProducts(Long branchId, int days) {
        Branch b = branch(branchId);
        return calc.topProducts(b.getStoreId(), b.getId(), Window.lastDays(AnalyticsCalculator.bounded(days, 1, 365, "days")), 5);
    }

    public List<TopCashierDto> topCashiers(Long branchId, int days) {
        Branch b = branch(branchId);
        return calc.topCashiers(b.getStoreId(), b.getId(), Window.lastDays(AnalyticsCalculator.bounded(days, 1, 365, "days")), 5);
    }

    public List<CategorySalesDto> categorySales(Long branchId, LocalDate date) {
        Branch b = branch(branchId);
        return calc.categorySales(b.getStoreId(), b.getId(), Window.day(date == null ? LocalDate.now() : date));
    }

    public List<PaymentBreakdownDto> paymentBreakdown(Long branchId, LocalDate date) {
        Branch b = branch(branchId);
        return calc.paymentBreakdown(b.getStoreId(), b.getId(), Window.day(date == null ? LocalDate.now() : date));
    }

    public BranchOverviewDto todayOverview(Long branchId) {
        Branch b = branch(branchId);
        LocalDate today = LocalDate.now();
        PeriodStatsDto now = calc.stats(b.getStoreId(), b.getId(), Window.day(today));
        PeriodStatsDto before = calc.stats(b.getStoreId(), b.getId(), Window.day(today.minusDays(1)));
        return new BranchOverviewDto(b.getId(), b.getName(), now, before,
                calc.growthPercent(now.grossSales(), before.grossSales()), calc.openShifts(b.getStoreId(), b.getId()));
    }

    public List<AlertDto> alerts(Long branchId) {
        Branch b = branch(branchId);
        return calc.alerts(b.getStoreId(), b.getId());
    }

    private Branch branch(Long branchId) {
        User actor = access.currentStoreUser();
        Long id = branchId;
        if (id == null) {
            if (actor.getRole() != UserRole.ROLE_BRANCH_MANAGER) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branchId is required");
            }
            id = actor.getBranchId();
        }
        return access.accessibleBranch(id);
    }
}
