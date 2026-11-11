package com.pos.saas.payload.request;

import com.pos.saas.domain.DiscountType;
import com.pos.saas.domain.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Prices are NOT sent by the client: the server reads them from the products.
 * The branch is the logged-in cashier's branch.
 */
@Data
public class OrderRequest {

    @NotEmpty(message = "An order needs at least one item")
    @Size(max = 200, message = "Too many items in one order")
    private List<@Valid OrderItemRequest> items;

    private Long customerId;                       // optional

    @NotNull(message = "paymentType is required (CASH, CARD or UPI)")
    private PaymentType paymentType;

    private DiscountType discountType;             // optional: PERCENTAGE or FIXED

    @DecimalMin(value = "0.01", message = "Discount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Discount must have at most 2 decimal places")
    private BigDecimal discountValue;              // required when discountType is set

    @Size(max = 500)
    private String notes;
}
