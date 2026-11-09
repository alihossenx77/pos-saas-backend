package com.pos.saas.controller;

import com.pos.saas.payload.dto.BranchDto;
import com.pos.saas.payload.request.BranchRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<BranchDto> create(@Valid @RequestBody BranchRequest request) {
        return ApiResponse.created("Branch created", branchService.create(request));
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<List<BranchDto>> byStore(@PathVariable Long storeId) {
        return ApiResponse.ok(branchService.listByStore(storeId));
    }

    @GetMapping("/{id}")
    public ApiResponse<BranchDto> get(@PathVariable Long id) {
        return ApiResponse.ok(branchService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ApiResponse<BranchDto> update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return ApiResponse.ok("Branch updated", branchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ApiResponse.message("Branch deleted");
    }
}
