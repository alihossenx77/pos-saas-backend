package com.pos.saas.repository;

import com.pos.saas.domain.OrderStatus;
import com.pos.saas.domain.PaymentType;
import com.pos.saas.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndStoreId(Long id, Long storeId);

    /** Row lock: used by refund and delete so two requests can't change the same order at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SalesOrder o where o.id = :id and o.storeId = :storeId")
    Optional<Order> findForUpdate(@Param("id") Long id, @Param("storeId") Long storeId);

    boolean existsByStoreId(Long storeId);
    boolean existsByBranchId(Long branchId);
    boolean existsByCashierId(Long cashierId);
    boolean existsByCustomerId(Long customerId);

    @Query("""
           select o from SalesOrder o
           where o.branchId = :branchId
             and (:customerId is null or o.customer.id = :customerId)
             and (:cashierId is null or o.cashier.id = :cashierId)
             and (:paymentType is null or o.paymentType = :paymentType)
             and (:status is null or o.status = :status)
           order by o.createdAt desc
           """)
    List<Order> filterByBranch(@Param("branchId") Long branchId,
                               @Param("customerId") Long customerId,
                               @Param("cashierId") Long cashierId,
                               @Param("paymentType") PaymentType paymentType,
                               @Param("status") OrderStatus status,
                               Pageable pageable);

    List<Order> findByCashierIdAndStoreIdOrderByCreatedAtDesc(Long cashierId, Long storeId, Pageable pageable);

    List<Order> findByBranchIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Long branchId, LocalDateTime from, LocalDateTime to);

    List<Order> findTop5ByBranchIdOrderByCreatedAtDesc(Long branchId);

    @Query("""
           select o from SalesOrder o
           where o.customer.id = :customerId and o.storeId = :storeId
             and (:branchId is null or o.branchId = :branchId)
           order by o.createdAt desc
           """)
    List<Order> findByCustomer(@Param("customerId") Long customerId,
                               @Param("storeId") Long storeId,
                               @Param("branchId") Long branchId,
                               Pageable pageable);

    // ---------- shift report aggregates (orders created by a cashier inside a time window) ----------

    long countByCashierIdAndCreatedAtBetween(Long cashierId, LocalDateTime from, LocalDateTime to);

    List<Order> findTop5ByCashierIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long cashierId, LocalDateTime from, LocalDateTime to);

    @Query("""
           select sum(o.totalAmount) from SalesOrder o
           where o.cashier.id = :cashierId and o.createdAt between :fromTime and :toTime
           """)
    BigDecimal sumTotalByCashier(@Param("cashierId") Long cashierId,
                                 @Param("fromTime") LocalDateTime fromTime,
                                 @Param("toTime") LocalDateTime toTime);

    /** rows: [PaymentType, Long orderCount, BigDecimal salesAmount] */
    @Query("""
           select o.paymentType, count(o), sum(o.totalAmount) from SalesOrder o
           where o.cashier.id = :cashierId and o.createdAt between :fromTime and :toTime
           group by o.paymentType
           """)
    List<Object[]> paymentBreakdown(@Param("cashierId") Long cashierId,
                                    @Param("fromTime") LocalDateTime fromTime,
                                    @Param("toTime") LocalDateTime toTime);

    /** rows: [Long productId, String name, String sku, Long quantitySold, BigDecimal grossRevenue] */
    @Query("""
           select i.productId, i.productName, i.sku, sum(i.quantity), sum(i.lineTotal)
           from SalesOrder o join o.items i
           where o.cashier.id = :cashierId and o.createdAt between :fromTime and :toTime
           group by i.productId, i.productName, i.sku
           order by sum(i.quantity) desc
           """)
    List<Object[]> topProducts(@Param("cashierId") Long cashierId,
                               @Param("fromTime") LocalDateTime fromTime,
                               @Param("toTime") LocalDateTime toTime,
                               Pageable pageable);
}
