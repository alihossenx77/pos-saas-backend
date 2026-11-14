package com.pos.saas.payload.dto.analytics;

import com.pos.saas.domain.PaymentType;

import java.math.BigDecimal;

/** percentage = share of gross sales. netAmount = salesAmount - refundAmount. */
public record PaymentBreakdownDto(PaymentType paymentType, long orderCount, BigDecimal salesAmount,
                                  long refundCount, BigDecimal refundAmount, BigDecimal netAmount,
                                  BigDecimal percentage) {}
