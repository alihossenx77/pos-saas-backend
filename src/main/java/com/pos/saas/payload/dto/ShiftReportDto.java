package com.pos.saas.payload.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lists return the summary only (the 4 detail lists are null).
 * "current", "{id}" and "by-date" return the full report.
 */
@Data @Builder
public class ShiftReportDto {
    private Long id;
    private Long storeId;
    private Long branchId;
    private Long cashierId;
    private String cashierName;
    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;        // null while open
    private String status;                 // OPEN or CLOSED

    private int totalOrders;
    private BigDecimal totalSales;
    private BigDecimal totalRefunds;
    private BigDecimal netSales;           // totalSales - totalRefunds

    private List<PaymentSummaryDto> paymentSummaries;
    private List<TopProductDto> topSellingProducts;
    private List<OrderDto> recentOrders;
    private List<RefundDto> refunds;
}
