package com.pos.saas.controller;

import com.pos.saas.domain.StoreStatus;
import com.pos.saas.payload.dto.StoreDto;
import com.pos.saas.payload.request.StoreRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /** A store admin owns exactly ONE store. Store names may repeat across different admins. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<StoreDto> create(@Valid @RequestBody StoreRequest request) {
        return ApiResponse.created("Store created", storeService.create(request));
    }

    /** The store owned by the logged-in store admin. */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<StoreDto> myStore() {
        return ApiResponse.ok(storeService.myStore());
    }

    /** The store the logged-in branch manager / cashier works in. */
    @GetMapping("/employee")
    @PreAuthorize("hasAnyAuthority('ROLE_BRANCH_MANAGER','ROLE_CASHIER')")
    public ApiResponse<StoreDto> employeeStore() {
        return ApiResponse.ok(storeService.employeeStore());
    }

    @GetMapping("/{id}")
    public ApiResponse<StoreDto> get(@PathVariable Long id) {
        return ApiResponse.ok(storeService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<StoreDto> update(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        return ApiResponse.ok("Store updated", storeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ApiResponse.message("Store deleted");
    }

    // ---- super admin ----

    /** GET /api/stores?status=PENDING */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<List<StoreDto>> all(@RequestParam(required = false) StoreStatus status) {
        return ApiResponse.ok(storeService.all(status));
    }

    /** PUT /api/stores/5/moderate?action=APPROVED */
    @PutMapping("/{id}/moderate")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<StoreDto> moderate(@PathVariable Long id, @RequestParam StoreStatus action) {
        return ApiResponse.ok("Store " + action.name().toLowerCase(), storeService.moderate(id, action));
    }
}
