package com.pos.saas.controller;

import com.pos.saas.payload.dto.RefundDto;
import com.pos.saas.payload.request.RefundRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER','ROLE_CASHIER')")
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RefundDto> create(@Valid @RequestBody RefundRequest request) {
        return ApiResponse.created("Refund processed", refundService.create(request));
    }

    /** All refunds of the store (admin view). */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<List<RefundDto>> all(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(refundService.listAll(page, size));
    }

    @GetMapping("/branch/{branchId}")
    public ApiResponse<List<RefundDto>> byBranch(@PathVariable Long branchId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(refundService.byBranch(branchId, page, size));
    }

    @GetMapping("/cashier/{cashierId}")
    public ApiResponse<List<RefundDto>> byCashier(@PathVariable Long cashierId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(refundService.byCashier(cashierId, page, size));
    }

    /** GET /api/refunds/cashier/5/range?from=2026-11-01T00:00:00&to=2026-11-08T23:59:59 */
    @GetMapping("/cashier/{cashierId}/range")
    public ApiResponse<List<RefundDto>> byCashierInRange(
            @PathVariable Long cashierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.ok(refundService.byCashierInRange(cashierId, from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<RefundDto> get(@PathVariable Long id) {
        return ApiResponse.ok(refundService.get(id));
    }
}
