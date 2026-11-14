package com.pos.saas.controller;

import com.pos.saas.payload.dto.analytics.*;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.BranchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Branch dashboard. Store admin must pass ?branchId=; a branch manager may leave it out (own branch).
 */
@RestController
@RequestMapping("/api/branch-analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
public class BranchAnalyticsController {

    private final BranchAnalyticsService service;

    /** Last N days (default 7) for the sales line chart, days with no sales are 0. */
    @GetMapping("/daily-sales")
    public ApiResponse<List<SalesPointDto>> dailySales(@RequestParam(required = false) Long branchId,
                                                       @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(service.dailySales(branchId, days));
    }

    /** Top 5 products by quantity (last `days`, default 30) with their share of revenue. */
    @GetMapping("/top-products")
    public ApiResponse<List<TopProductStatDto>> topProducts(@RequestParam(required = false) Long branchId,
                                                            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.topProducts(branchId, days));
    }

    /** Top 5 cashiers by revenue (last `days`, default 30). */
    @GetMapping("/top-cashiers")
    public ApiResponse<List<TopCashierDto>> topCashiers(@RequestParam(required = false) Long branchId,
                                                        @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.topCashiers(branchId, days));
    }

    /** Category-wise sales of one date (default today). */
    @GetMapping("/category-sales")
    public ApiResponse<List<CategorySalesDto>> categorySales(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(service.categorySales(branchId, date));
    }

    /** Payment-method breakdown of one date (default today). */
    @GetMapping("/payment-breakdown")
    public ApiResponse<List<PaymentBreakdownDto>> paymentBreakdown(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(service.paymentBreakdown(branchId, date));
    }

    /** Today vs yesterday: orders, sales, refunds, customers, open shifts. */
    @GetMapping("/today-overview")
    public ApiResponse<BranchOverviewDto> todayOverview(@RequestParam(required = false) Long branchId) {
        return ApiResponse.ok(service.todayOverview(branchId));
    }

    /** Low stock, out of stock, forgotten shifts, inactive employees. */
    @GetMapping("/alerts")
    public ApiResponse<List<AlertDto>> alerts(@RequestParam(required = false) Long branchId) {
        return ApiResponse.ok(service.alerts(branchId));
    }
}
