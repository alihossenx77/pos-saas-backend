package com.pos.saas.controller;

import com.pos.saas.domain.UserRole;
import com.pos.saas.payload.dto.UserDto;
import com.pos.saas.payload.request.EmployeeRequest;
import com.pos.saas.payload.request.EmployeeUpdateRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto> create(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.created("Employee created", employeeService.create(request));
    }

    @PostMapping("/branch/{branchId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto> createForBranch(@PathVariable Long branchId,
                                                @Valid @RequestBody EmployeeRequest request) {
        request.setBranchId(branchId);
        return ApiResponse.created("Employee created", employeeService.create(request));
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<List<UserDto>> byStore(@PathVariable Long storeId,
                                              @RequestParam(required = false) String role) {
        return ApiResponse.ok(employeeService.listByStore(storeId, parseRole(role)));
    }

    /** GET /api/employees/branch/3?role=MANAGER (role optional: CASHIER, MANAGER, ROLE_CASHIER ...) */
    @GetMapping("/branch/{branchId}")
    public ApiResponse<List<UserDto>> byBranch(@PathVariable Long branchId,
                                               @RequestParam(required = false) String role) {
        return ApiResponse.ok(employeeService.listByBranch(branchId, parseRole(role)));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDto> get(@PathVariable Long id) {
        return ApiResponse.ok(employeeService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserDto> update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return ApiResponse.ok("Employee updated", employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ApiResponse.message("Employee deleted");
    }

    private UserRole parseRole(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.trim().toUpperCase();
        if (!v.startsWith("ROLE_")) v = "ROLE_" + v;
        if (v.equals("ROLE_MANAGER")) v = "ROLE_BRANCH_MANAGER";
        try {
            return UserRole.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + raw);
        }
    }
}
