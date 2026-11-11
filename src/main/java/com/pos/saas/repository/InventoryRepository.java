package com.pos.saas.repository;

import com.pos.saas.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByIdAndStoreId(Long id, Long storeId);
    List<Inventory> findByBranchId(Long branchId);
    List<Inventory> findByProductIdAndStoreId(Long productId, Long storeId);
    boolean existsByBranchId(Long branchId);
    boolean existsByBranchIdAndProductId(Long branchId, Long productId);
    void deleteByProductId(Long productId);

    /** Row lock so two cashiers can't sell the last unit at the same moment. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.branchId = :branchId and i.product.id = :productId")
    Optional<Inventory> findForUpdate(@Param("branchId") Long branchId, @Param("productId") Long productId);
}
