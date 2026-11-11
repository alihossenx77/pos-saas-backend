package com.pos.saas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A cashier's working period. While open (shiftEnd == null) the totals are calculated live;
 * when the shift is closed the totals are saved here so the history never changes.
 * Orders and refunds belong to a shift by time: created by this cashier between shiftStart and shiftEnd.
 */
@Entity
@Table(name = "shift_reports", indexes = {
        @Index(columnList = "store_id"),
        @Index(columnList = "branch_id"),
        @Index(columnList = "cashier_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cashier_id")
    private User cashier;

    @Column(nullable = false)
    private LocalDateTime shiftStart;

    /** Null while the shift is still open. */
    private LocalDateTime shiftEnd;

    // ---- saved when the shift is closed ----
    private Integer totalOrders;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalSales;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalRefunds;

    @Column(precision = 14, scale = 2)
    private BigDecimal netSales;
}
