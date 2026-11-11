package com.pos.saas.repository;

import com.pos.saas.model.Refund;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByIdAndStoreId(Long id, Long storeId);
    boolean existsByOrderId(Long orderId);
    boolean existsByCashierId(Long cashierId);

    List<Refund> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);
    List<Refund> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);
    List<Refund> findByCashierIdAndStoreIdOrderByCreatedAtDesc(Long cashierId, Long storeId, Pageable pageable);
    List<Refund> findByCashierIdAndStoreIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long cashierId, Long storeId, LocalDateTime from, LocalDateTime to);
}
