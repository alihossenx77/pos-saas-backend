package com.pos.saas.controller;

import com.pos.saas.domain.UserRole;
import com.pos.saas.payload.UserDto;
import com.pos.saas.payload.request.EmployeeRequest;
import com.pos.saas.payload.request.EmployeeUpdateRequest;
import com.pos.saas.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PostMapping("/branch/{branchId}")
    public ResponseEntity<UserDto> createForBranch(@PathVariable Long branchId,
                                                   @Valid @RequestBody EmployeeRequest request) {
        request.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<UserDto>> byStore(@PathVariable Long storeId,
                                                 @RequestParam(required = false) String role) {
        return ResponseEntity.ok(employeeService.listByStore(storeId, parseRole(role)));
    }

    /** GET /api/employees/branch/3?role=MANAGER (role optional: CASHIER, MANAGER, ROLE_CASHIER ...) */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<UserDto>> byBranch(@PathVariable Long branchId,
                                                  @RequestParam(required = false) String role) {
        return ResponseEntity.ok(employeeService.listByBranch(branchId, parseRole(role)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id,
                                          @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Employee deleted"));
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