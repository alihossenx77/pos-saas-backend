package com.pos.saas.payload.dto.analytics;

import java.math.BigDecimal;

/** revenue is before order discounts. Deleted / uncategorised products fall under "Uncategorized". */
public record CategorySalesDto(String categoryName, long quantitySold, BigDecimal revenue, BigDecimal percentage) {}
