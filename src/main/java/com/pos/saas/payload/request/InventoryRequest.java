package com.pos.saas.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryRequest {
    @NotNull(message = "branchId is required") private Long branchId;
    @NotNull(message = "productId is required") private Long productId;

    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Max(value = 1_000_000, message = "Quantity is too large")
    private Integer quantity;
}
