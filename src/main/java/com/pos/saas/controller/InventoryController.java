package com.pos.saas.controller;

import com.pos.saas.payload.dto.InventoryDto;
import com.pos.saas.payload.request.InventoryRequest;
import com.pos.saas.payload.request.InventoryUpdateRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ApiResponse<InventoryDto> create(@Valid @RequestBody InventoryRequest request) {
        return ApiResponse.created("Stock added", inventoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ApiResponse<InventoryDto> update(@PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest request) {
        return ApiResponse.ok("Stock updated", inventoryService.updateQuantity(id, request.getQuantity()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ApiResponse.message("Inventory record deleted");
    }

    @GetMapping("/{id}")
    public ApiResponse<InventoryDto> get(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.get(id));
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<List<InventoryDto>> byProduct(@PathVariable Long productId) {
        return ApiResponse.ok(inventoryService.byProduct(productId));
    }

    @GetMapping("/branch/{branchId}")
    public ApiResponse<List<InventoryDto>> byBranch(@PathVariable Long branchId) {
        return ApiResponse.ok(inventoryService.byBranch(branchId));
    }

    /** GET /api/inventories/branch/1/low-stock?threshold=10 */
    @GetMapping("/branch/{branchId}/low-stock")
    public ApiResponse<List<InventoryDto>> lowStock(@PathVariable Long branchId,
                                                    @RequestParam(defaultValue = "10") int threshold) {
        return ApiResponse.ok(inventoryService.lowStock(branchId, threshold));
    }
}
