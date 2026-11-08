package com.pos.saas.controller;

import com.pos.saas.domain.StoreStatus;

import com.pos.saas.payload.dto.StoreDto;
import com.pos.saas.payload.request.StoreRequest;
import com.pos.saas.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /** Create a store (a store admin can create many, names may repeat). */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ResponseEntity<StoreDto> create(@Valid @RequestBody StoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.create(request));
    }

    /** All stores owned by the logged-in store admin. */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ResponseEntity<List<StoreDto>> myStores() {
        return ResponseEntity.ok(Collections.singletonList(storeService.myStore()));
    }

    /** The store the logged-in branch manager / cashier works in. */
    @GetMapping("/employee")
    @PreAuthorize("hasAnyAuthority('ROLE_BRANCH_MANAGER','ROLE_CASHIER')")
    public ResponseEntity<StoreDto> employeeStore() {
        return ResponseEntity.ok(storeService.employeeStore());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ResponseEntity<StoreDto> update(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        return ResponseEntity.ok(storeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Store deleted"));
    }

    // ---- super admin ----

    /** GET /api/stores?status=PENDING */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<StoreDto>> all(@RequestParam(required = false) StoreStatus status) {
        return ResponseEntity.ok(storeService.all(status));
    }

    /** PUT /api/stores/5/moderate?action=APPROVED */
    @PutMapping("/{id}/moderate")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<StoreDto> moderate(@PathVariable Long id, @RequestParam StoreStatus action) {
        return ResponseEntity.ok(storeService.moderate(id, action));
    }
}