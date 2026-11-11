package com.pos.saas.repository;

import com.pos.saas.model.Refund;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    // ---------- shift report aggregates (refunds processed by a cashier inside a time window) ----------

    @Query("""
           select sum(r.amount) from Refund r
           where r.cashier.id = :cashierId and r.createdAt between :fromTime and :toTime
           """)
    BigDecimal sumAmountByCashier(@Param("cashierId") Long cashierId,
                                  @Param("fromTime") LocalDateTime fromTime,
                                  @Param("toTime") LocalDateTime toTime);

    /** rows: [PaymentType, Long refundCount, BigDecimal refundAmount] */
    @Query("""
           select r.paymentType, count(r), sum(r.amount) from Refund r
           where r.cashier.id = :cashierId and r.createdAt between :fromTime and :toTime
           group by r.paymentType
           """)
    List<Object[]> paymentBreakdown(@Param("cashierId") Long cashierId,
                                    @Param("fromTime") LocalDateTime fromTime,
                                    @Param("toTime") LocalDateTime toTime);
}
