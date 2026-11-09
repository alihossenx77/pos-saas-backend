package com.pos.saas.payload.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** Create a product (all required fields must be present). */
@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255)
    private String name;

    @NotBlank(message = "SKU / barcode is required")
    @Size(max = 100)
    private String sku;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "MRP is required")
    @DecimalMin(value = "0.0", message = "MRP cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "MRP must have at most 2 decimal places")
    private BigDecimal mrp;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", message = "Selling price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Selling price must have at most 2 decimal places")
    private BigDecimal sellingPrice;

    @Size(max = 100)
    private String brand;

    @Size(max = 500)
    private String image;

    private Long categoryId;
}
