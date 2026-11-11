package com.pos.saas.controller;

import com.pos.saas.payload.dto.CustomerDto;
import com.pos.saas.payload.request.CustomerRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER','ROLE_CASHIER')")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerDto> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.created("Customer created", customerService.create(request));
    }

    /** GET /api/customers?q=ali&page=0&size=50  (q = name / phone / email, optional) */
    @GetMapping
    public ApiResponse<List<CustomerDto>> list(@RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(customerService.list(q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerDto> get(@PathVariable Long id) {
        return ApiResponse.ok(customerService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerDto> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok("Customer updated", customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.message("Customer deleted");
    }
}
