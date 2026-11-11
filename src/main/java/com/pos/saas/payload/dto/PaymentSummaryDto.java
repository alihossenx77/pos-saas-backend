package com.pos.saas.payload.dto;

import com.pos.saas.domain.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** One row per payment type used in the shift: handy for cash-drawer reconciliation. */
@Data @Builder
public class PaymentSummaryDto {
    private PaymentType paymentType;
    private long orderCount;
    private BigDecimal salesAmount;
    private long refundCount;
    private BigDecimal refundAmount;
    private BigDecimal netAmount;      // salesAmount - refundAmount
}
