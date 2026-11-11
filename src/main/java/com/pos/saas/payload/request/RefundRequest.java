package com.pos.saas.payload.request;

import com.pos.saas.domain.PaymentType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotBlank(message = "A reason is required")
    @Size(max = 500)
    private String reason;

    /** Leave empty to refund the whole remaining amount. */
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    /** Optional: defaults to the order's payment type. */
    private PaymentType paymentType;
}
