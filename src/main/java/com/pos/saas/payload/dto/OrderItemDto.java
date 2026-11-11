package com.pos.saas.payload.dto;

import com.pos.saas.model.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemDto {
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public static OrderItemDto from(OrderItem i) {
        return OrderItemDto.builder()
                .productId(i.getProductId())
                .productName(i.getProductName())
                .sku(i.getSku())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .lineTotal(i.getLineTotal())
                .build();
    }
}