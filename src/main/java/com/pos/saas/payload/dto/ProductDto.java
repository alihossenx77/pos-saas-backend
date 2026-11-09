package com.pos.saas.payload.dto;

import com.pos.saas.model.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ProductDto {
    private Long id;
    private String name;
    private String sku;
    private String description;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private String brand;
    private String image;
    private Long categoryId;
    private String categoryName;
    private Long storeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductDto from(Product p) {
        return ProductDto.builder()
                .id(p.getId()).name(p.getName()).sku(p.getSku()).description(p.getDescription())
                .mrp(p.getMrp()).sellingPrice(p.getSellingPrice()).brand(p.getBrand()).image(p.getImage())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .storeId(p.getStoreId())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}
