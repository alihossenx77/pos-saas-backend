package com.pos.saas.controller;

import com.pos.saas.payload.dto.BranchDto;
import com.pos.saas.payload.request.BranchRequest;
import com.pos.saas.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ResponseEntity<BranchDto> create(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(request));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<BranchDto>> byStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(branchService.listByStore(storeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ResponseEntity<BranchDto> update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(branchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Branch deleted"));
    }
}