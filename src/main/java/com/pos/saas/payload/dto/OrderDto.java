package com.pos.saas.payload.dto;

import com.pos.saas.domain.DiscountType;
import com.pos.saas.domain.OrderStatus;
import com.pos.saas.domain.PaymentType;
import com.pos.saas.model.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class OrderDto {
    private Long id;
    private Long storeId;
    private Long branchId;
    private Long cashierId;
    private String cashierName;
    private Long customerId;
    private String customerName;
    private PaymentType paymentType;
    private OrderStatus status;
    private BigDecimal subtotal;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal refundedAmount;
    private String notes;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;

    public static OrderDto from(Order o) {
        return OrderDto.builder()
                .id(o.getId())
                .storeId(o.getStoreId())
                .branchId(o.getBranchId())
                .cashierId(o.getCashier().getId())
                .cashierName(o.getCashier().getFullName())
                .customerId(o.getCustomer() != null ? o.getCustomer().getId() : null)
                .customerName(o.getCustomer() != null ? o.getCustomer().getFullName() : null)
                .paymentType(o.getPaymentType())
                .status(o.getStatus())
                .subtotal(o.getSubtotal())
                .discountType(o.getDiscountType())
                .discountValue(o.getDiscountValue())
                .discountAmount(o.getDiscountAmount())
                .totalAmount(o.getTotalAmount())
                .refundedAmount(o.getRefundedAmount())
                .notes(o.getNotes())
                .items(o.getItems().stream().map(OrderItemDto::from).toList())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
