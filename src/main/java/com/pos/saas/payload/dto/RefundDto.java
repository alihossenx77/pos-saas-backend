package com.pos.saas.payload.dto;

import com.pos.saas.domain.PaymentType;
import com.pos.saas.model.Refund;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class RefundDto {
    private Long id;
    private Long orderId;
    private Long storeId;
    private Long branchId;
    private Long cashierId;
    private String cashierName;
    private BigDecimal amount;
    private String reason;
    private PaymentType paymentType;
    private LocalDateTime createdAt;

    public static RefundDto from(Refund r) {
        return RefundDto.builder()
                .id(r.getId())
                .orderId(r.getOrderId())
                .storeId(r.getStoreId())
                .branchId(r.getBranchId())
                .cashierId(r.getCashier().getId())
                .cashierName(r.getCashier().getFullName())
                .amount(r.getAmount())
                .reason(r.getReason())
                .paymentType(r.getPaymentType())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
