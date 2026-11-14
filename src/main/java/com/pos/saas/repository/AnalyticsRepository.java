package com.pos.saas.repository;

import com.pos.saas.model.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only aggregate queries for the dashboards.
 * Every query is scoped by storeId, optionally by branchId (null = all branches) and a time window
 * [fromTime, toTime) (from inclusive, to exclusive).
 * The date-grouping queries are native MySQL (DATE(), DATE_FORMAT(), WEEKDAY()).
 */
public interface AnalyticsRepository extends Repository<Order, Long> {

    // ------------------------------------------------------------ totals

    /** one row: [Long orders, BigDecimal grossSales (nullable), Long distinctCustomers] */
    @Query("""
           select count(o), sum(o.totalAmount), count(distinct o.customer.id)
           from SalesOrder o
           where o.storeId = :storeId and (:branchId is null or o.branchId = :branchId)
             and o.createdAt >= :fromTime and o.createdAt < :toTime
           """)
    List<Object[]> orderTotals(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                               @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    /** one row: [Long refunds, BigDecimal refundedAmount (nullable)] */
    @Query("""
           select count(r), sum(r.amount)
           from Refund r
           where r.storeId = :storeId and (:branchId is null or r.branchId = :branchId)
             and r.createdAt >= :fromTime and r.createdAt < :toTime
           """)
    List<Object[]> refundTotals(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    // ------------------------------------------------------------ payment types

    /** rows: [PaymentType, Long orders, BigDecimal sales] */
    @Query("""
           select o.paymentType, count(o), sum(o.totalAmount)
           from SalesOrder o
           where o.storeId = :storeId and (:branchId is null or o.branchId = :branchId)
             and o.createdAt >= :fromTime and o.createdAt < :toTime
           group by o.paymentType
           """)
    List<Object[]> orderPaymentBreakdown(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                         @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    /** rows: [PaymentType, Long refunds, BigDecimal amount] */
    @Query("""
           select r.paymentType, count(r), sum(r.amount)
           from Refund r
           where r.storeId = :storeId and (:branchId is null or r.branchId = :branchId)
             and r.createdAt >= :fromTime and r.createdAt < :toTime
           group by r.paymentType
           """)
    List<Object[]> refundPaymentBreakdown(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                          @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    // ------------------------------------------------------------ products / cashiers / branches

    /** rows: [Long productId, String name, String sku, Long quantity, BigDecimal revenue], best first */
    @Query("""
           select i.productId, i.productName, i.sku, sum(i.quantity), sum(i.lineTotal)
           from SalesOrder o join o.items i
           where o.storeId = :storeId and (:branchId is null or o.branchId = :branchId)
             and o.createdAt >= :fromTime and o.createdAt < :toTime
           group by i.productId, i.productName, i.sku
           order by sum(i.quantity) desc
           """)
    List<Object[]> topProducts(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                               @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime,
                               Pageable pageable);

    /** revenue of ALL items in the window (for percentages) */
    @Query("""
           select sum(i.lineTotal)
           from SalesOrder o join o.items i
           where o.storeId = :storeId and (:branchId is null or o.branchId = :branchId)
             and o.createdAt >= :fromTime and o.createdAt < :toTime
           """)
    java.math.BigDecimal itemsRevenue(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                      @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    /** rows: [Long productId, Long quantity, BigDecimal revenue] (grouped into categories in Java) */
    @Query("""
           select i.productId, sum(i.quantity), sum(i.lineTotal)
           from SalesOrder o join o.items i
           where o.storeId = :storeId and (:branchId is null or o.branchId = :branchId)
             and o.createdAt >= :fromTime and o.createdAt < :toTime
           group by i.productId
           """)
    List<Object[]> productTotals(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                 @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    /** rows: [Long cashierId, String name, Long orders, BigDecimal revenue], best first */
    @Query("""
           select o.cashier.id, o.cashier.fullName, count(o), sum(o.totalAmount)
           from SalesOrder o
           where o.storeId = :storeId and (:branchId is null or o.branchId = :branchId)
             and o.createdAt >= :fromTime and o.createdAt < :toTime
           group by o.cashier.id, o.cashier.fullName
           order by sum(o.totalAmount) desc
           """)
    List<Object[]> topCashiers(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                               @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime,
                               Pageable pageable);

    /** rows: [Long branchId, Long orders, BigDecimal sales] */
    @Query("""
           select o.branchId, count(o), sum(o.totalAmount)
           from SalesOrder o
           where o.storeId = :storeId and o.createdAt >= :fromTime and o.createdAt < :toTime
           group by o.branchId
           """)
    List<Object[]> branchOrderTotals(@Param("storeId") Long storeId,
                                     @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    /** rows: [Long branchId, BigDecimal refunded] */
    @Query("""
           select r.branchId, sum(r.amount)
           from Refund r
           where r.storeId = :storeId and r.createdAt >= :fromTime and r.createdAt < :toTime
           group by r.branchId
           """)
    List<Object[]> branchRefundTotals(@Param("storeId") Long storeId,
                                      @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    // ------------------------------------------------------------ time series (native MySQL)

    /** rows: [Date day, Long orders, BigDecimal sales] */
    @Query(value = """
           SELECT DATE(o.created_at) AS d, COUNT(*) AS cnt, COALESCE(SUM(o.total_amount), 0) AS total
           FROM orders o
           WHERE o.store_id = :storeId AND (:branchId IS NULL OR o.branch_id = :branchId)
             AND o.created_at >= :fromTime AND o.created_at < :toTime
           GROUP BY d ORDER BY d
           """, nativeQuery = true)
    List<Object[]> dailyOrders(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                               @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    /** rows: [Date day, Long refunds, BigDecimal amount] */
    @Query(value = """
           SELECT DATE(r.created_at) AS d, COUNT(*) AS cnt, COALESCE(SUM(r.amount), 0) AS total
           FROM refunds r
           WHERE r.store_id = :storeId AND (:branchId IS NULL OR r.branch_id = :branchId)
             AND r.created_at >= :fromTime AND r.created_at < :toTime
           GROUP BY d ORDER BY d
           """, nativeQuery = true)
    List<Object[]> dailyRefunds(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    /** rows: [Date monday, Long orders, BigDecimal sales] */
    @Query(value = """
           SELECT DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) AS week_start,
                  COUNT(*) AS cnt, COALESCE(SUM(o.total_amount), 0) AS total
           FROM orders o
           WHERE o.store_id = :storeId AND (:branchId IS NULL OR o.branch_id = :branchId)
             AND o.created_at >= :fromTime AND o.created_at < :toTime
           GROUP BY week_start ORDER BY week_start
           """, nativeQuery = true)
    List<Object[]> weeklyOrders(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    @Query(value = """
           SELECT DATE_SUB(DATE(r.created_at), INTERVAL WEEKDAY(r.created_at) DAY) AS week_start,
                  COUNT(*) AS cnt, COALESCE(SUM(r.amount), 0) AS total
           FROM refunds r
           WHERE r.store_id = :storeId AND (:branchId IS NULL OR r.branch_id = :branchId)
             AND r.created_at >= :fromTime AND r.created_at < :toTime
           GROUP BY week_start ORDER BY week_start
           """, nativeQuery = true)
    List<Object[]> weeklyRefunds(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                 @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    /** rows: [String "2026-11", Long orders, BigDecimal sales] */
    @Query(value = """
           SELECT DATE_FORMAT(o.created_at, '%Y-%m') AS ym, COUNT(*) AS cnt, COALESCE(SUM(o.total_amount), 0) AS total
           FROM orders o
           WHERE o.store_id = :storeId AND (:branchId IS NULL OR o.branch_id = :branchId)
             AND o.created_at >= :fromTime AND o.created_at < :toTime
           GROUP BY ym ORDER BY ym
           """, nativeQuery = true)
    List<Object[]> monthlyOrders(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                 @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    @Query(value = """
           SELECT DATE_FORMAT(r.created_at, '%Y-%m') AS ym, COUNT(*) AS cnt, COALESCE(SUM(r.amount), 0) AS total
           FROM refunds r
           WHERE r.store_id = :storeId AND (:branchId IS NULL OR r.branch_id = :branchId)
             AND r.created_at >= :fromTime AND r.created_at < :toTime
           GROUP BY ym ORDER BY ym
           """, nativeQuery = true)
    List<Object[]> monthlyRefunds(@Param("storeId") Long storeId, @Param("branchId") Long branchId,
                                  @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

    // ------------------------------------------------------------ super admin

    /** rows: [Date day, Long stores registered that day] */
    @Query(value = """
           SELECT DATE(s.created_at) AS d, COUNT(*) AS cnt
           FROM stores s
           WHERE s.created_at >= :fromTime AND s.created_at < :toTime
           GROUP BY d ORDER BY d
           """, nativeQuery = true)
    List<Object[]> storeRegistrationsDaily(@Param("fromTime") LocalDateTime fromTime,
                                           @Param("toTime") LocalDateTime toTime);
}
