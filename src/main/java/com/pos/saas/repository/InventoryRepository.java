package com.pos.saas.repository;

import com.pos.saas.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByIdAndStoreId(Long id, Long storeId);
    List<Inventory> findByBranchId(Long branchId);
    List<Inventory> findByProductIdAndStoreId(Long productId, Long storeId);
    boolean existsByBranchId(Long branchId);
    boolean existsByBranchIdAndProductId(Long branchId, Long productId);
    void deleteByProductId(Long productId);
}
