package com.pos.saas.payload.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class TopProductDto {
    private Long productId;
    private String productName;
    private String sku;
    private long quantitySold;
    private BigDecimal grossRevenue;   // before order-level discounts
}
