package com.pos.saas.payload.dto.analytics;

import java.math.BigDecimal;

/** percentage = this product's revenue / revenue of all products in the period (before order discounts). */
public record TopProductStatDto(Long productId, String productName, String sku, long quantitySold,
                                BigDecimal revenue, BigDecimal percentage) {}
