package com.pos.saas.controller;

import com.pos.saas.payload.dto.ShiftReportDto;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.ShiftReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shift-reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER','ROLE_CASHIER')")
public class ShiftReportController {

    private final ShiftReportService shiftService;

    // ---- cashier ----

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CASHIER')")
    public ApiResponse<ShiftReportDto> start() {
        return ApiResponse.created("Shift started", shiftService.start());
    }

    @PatchMapping("/end")
    @PreAuthorize("hasAuthority('ROLE_CASHIER')")
    public ApiResponse<ShiftReportDto> end() {
        return ApiResponse.ok("Shift ended", shiftService.end());
    }

    /** Live progress of the running shift. */
    @GetMapping("/current")
    @PreAuthorize("hasAuthority('ROLE_CASHIER')")
    public ApiResponse<ShiftReportDto> current() {
        return ApiResponse.ok(shiftService.current());
    }

    // ---- manager / admin ----

    /** Close a shift the cashier forgot to end. */
    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ApiResponse<ShiftReportDto> forceClose(@PathVariable Long id) {
        return ApiResponse.ok("Shift closed", shiftService.forceClose(id));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ApiResponse<List<ShiftReportDto>> byBranch(@PathVariable Long branchId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(shiftService.byBranch(branchId, page, size));
    }

    /** Every shift of the store. */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<List<ShiftReportDto>> all(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(shiftService.all(page, size));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        shiftService.delete(id);
        return ApiResponse.message("Shift report deleted");
    }

    // ---- anyone allowed to see that cashier ----

    @GetMapping("/cashier/{cashierId}")
    public ApiResponse<List<ShiftReportDto>> byCashier(@PathVariable Long cashierId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(shiftService.byCashier(cashierId, page, size));
    }

    /** GET /api/shift-reports/cashier/5/by-date?date=2026-11-10 */
    @GetMapping("/cashier/{cashierId}/by-date")
    public ApiResponse<List<ShiftReportDto>> byDate(
            @PathVariable Long cashierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(shiftService.byCashierAndDate(cashierId, date));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShiftReportDto> get(@PathVariable Long id) {
        return ApiResponse.ok(shiftService.get(id));
    }
}
