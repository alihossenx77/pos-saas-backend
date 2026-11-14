package com.pos.saas.controller;

import com.pos.saas.payload.dto.analytics.DailyCountDto;
import com.pos.saas.payload.dto.analytics.StatusCountDto;
import com.pos.saas.payload.dto.analytics.SuperAdminSummaryDto;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.SuperAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminDashboardController {

    private final SuperAdminDashboardService service;

    /** Total / active / pending / declined / blocked stores. */
    @GetMapping("/summary")
    public ApiResponse<SuperAdminSummaryDto> summary() {
        return ApiResponse.ok(service.summary());
    }

    /** Stores registered per day, last 7 days by default. */
    @GetMapping("/store-registrations")
    public ApiResponse<List<DailyCountDto>> registrations(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(service.registrations(days));
    }

    /** For the pie chart. */
    @GetMapping("/store-status-distribution")
    public ApiResponse<List<StatusCountDto>> statusDistribution() {
        return ApiResponse.ok(service.statusDistribution());
    }
}
