package com.pos.saas.controller;

import com.pos.saas.payload.dto.analytics.*;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.StoreAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Store dashboard. {storeAdminId} must be the logged-in store admin's own id. */
@RestController
@RequestMapping("/api/store/analytics/{storeAdminId}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
public class StoreAnalyticsController {

    private final StoreAnalyticsService service;

    @GetMapping("/overview")
    public ApiResponse<StoreOverviewDto> overview(@PathVariable Long storeAdminId) {
        return ApiResponse.ok(service.overview(storeAdminId));
    }

    /** ?period=daily (30 days) | weekly (12 weeks) | monthly (12 months) */
    @GetMapping("/sales-trends")
    public ApiResponse<List<SalesPointDto>> trends(@PathVariable Long storeAdminId,
                                                   @RequestParam(defaultValue = "daily") String period) {
        return ApiResponse.ok(service.trends(storeAdminId, period));
    }

    @GetMapping("/sales/monthly")
    public ApiResponse<List<SalesPointDto>> monthly(@PathVariable Long storeAdminId,
                                                    @RequestParam(defaultValue = "12") int months) {
        return ApiResponse.ok(service.monthly(storeAdminId, months));
    }

    @GetMapping("/sales/daily")
    public ApiResponse<List<SalesPointDto>> daily(@PathVariable Long storeAdminId,
                                                  @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.daily(storeAdminId, days));
    }

    @GetMapping("/sales/category")
    public ApiResponse<List<CategorySalesDto>> category(@PathVariable Long storeAdminId,
                                                        @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.categories(storeAdminId, days));
    }

    @GetMapping("/sales/payment-method")
    public ApiResponse<List<PaymentBreakdownDto>> paymentMethod(@PathVariable Long storeAdminId,
                                                                @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.payments(storeAdminId, days));
    }

    /** Total sales per branch. */
    @GetMapping("/sales/branch")
    public ApiResponse<List<BranchPerformanceDto>> salesByBranch(@PathVariable Long storeAdminId,
                                                                 @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.branches(storeAdminId, days));
    }

    /** Detailed payment types: orders, sales, refunds, net, share. */
    @GetMapping("/payments")
    public ApiResponse<List<PaymentBreakdownDto>> payments(@PathVariable Long storeAdminId,
                                                           @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.payments(storeAdminId, days));
    }

    /** Orders, revenue, refunds, net and average order value for each branch. */
    @GetMapping("/branch-performance")
    public ApiResponse<List<BranchPerformanceDto>> branchPerformance(@PathVariable Long storeAdminId,
                                                                     @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.branches(storeAdminId, days));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertDto>> alerts(@PathVariable Long storeAdminId) {
        return ApiResponse.ok(service.alerts(storeAdminId));
    }
}
