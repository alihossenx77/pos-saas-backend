package com.pos.saas.payload.dto.analytics;

import java.math.BigDecimal;

public record TopCashierDto(Long cashierId, String cashierName, long orders, BigDecimal revenue) {}
