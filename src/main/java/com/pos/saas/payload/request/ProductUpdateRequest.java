package com.pos.saas.payload.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** PATCH / PUT: every field optional, only the ones you send are changed (but sent values are still validated). */
@Data
public class ProductUpdateRequest {

    @Size(min = 2, max = 255)
    private String name;

    @Size(min = 1, max = 100)
    private String sku;

    @Size(max = 1000)
    private String description;

    @DecimalMin(value = "0.0", message = "MRP cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "MRP must have at most 2 decimal places")
    private BigDecimal mrp;

    @DecimalMin(value = "0.0", message = "Selling price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Selling price must have at most 2 decimal places")
    private BigDecimal sellingPrice;

    @Size(max = 100)
    private String brand;

    @Size(max = 500)
    private String image;

    private Long categoryId;
}
