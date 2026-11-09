package com.pos.saas.service;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.Branch;
import com.pos.saas.model.Inventory;
import com.pos.saas.model.Product;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.InventoryDto;
import com.pos.saas.payload.request.InventoryRequest;
import com.pos.saas.repository.InventoryRepository;
import com.pos.saas.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Stock per branch.
 * STORE_ADMIN: any branch of the store. BRANCH_MANAGER: own branch. CASHIER: read-only on own branch.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final AccessService access;

    @Transactional
    public InventoryDto create(InventoryRequest req) {
        Branch branch = access.accessibleBranch(req.getBranchId());

        Product product = productRepository.findByIdAndStoreId(req.getProductId(), branch.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Product not found"));

        if (inventoryRepository.existsByBranchIdAndProductId(branch.getId(), product.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock already exists for this product in this branch. Update it instead.");
        }

        return InventoryDto.from(inventoryRepository.save(Inventory.builder()
                .branchId(branch.getId())
                .product(product)
                .quantity(req.getQuantity())
                .storeId(branch.getStoreId())
                .build()));
    }

    @Transactional
    public InventoryDto updateQuantity(Long id, int quantity) {
        Inventory inv = find(id);
        inv.setQuantity(quantity);
        return InventoryDto.from(inventoryRepository.save(inv));
    }

    @Transactional
    public void delete(Long id) {
        inventoryRepository.delete(find(id));
    }

    public InventoryDto get(Long id) {
        return InventoryDto.from(find(id));
    }

    /** Stock of one product across branches. Branch staff only see their own branch. */
    public List<InventoryDto> byProduct(Long productId) {
        User u = access.currentStoreUser();
        productRepository.findByIdAndStoreId(productId, u.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Product not found"));

        return inventoryRepository.findByProductIdAndStoreId(productId, u.getStoreId()).stream()
                .filter(i -> u.getRole() == UserRole.ROLE_STORE_ADMIN
                        || Objects.equals(i.getBranchId(), u.getBranchId()))
                .map(InventoryDto::from)
                .toList();
    }

    public List<InventoryDto> byBranch(Long branchId) {
        Branch branch = access.accessibleBranch(branchId);
        return inventoryRepository.findByBranchId(branch.getId()).stream().map(InventoryDto::from).toList();
    }

    /** Items at or below the threshold (feeds the "low stock alerts" widget). */
    public List<InventoryDto> lowStock(Long branchId, int threshold) {
        return byBranch(branchId).stream().filter(i -> i.getQuantity() <= threshold).toList();
    }

    // ---- helper: record must be in the caller's store AND in a branch the caller may access ----
    private Inventory find(Long id) {
        User u = access.currentStoreUser();
        Inventory inv = inventoryRepository.findByIdAndStoreId(id, u.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Inventory record not found"));
        access.accessibleBranch(inv.getBranchId());
        return inv;
    }
}
