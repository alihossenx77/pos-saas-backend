package com.pos.saas.payload.dto;

import com.pos.saas.model.Inventory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class InventoryDto {
    private Long id;
    private Long branchId;
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;
    private LocalDateTime lastUpdated;

    public static InventoryDto from(Inventory i) {
        return InventoryDto.builder()
                .id(i.getId()).branchId(i.getBranchId())
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .sku(i.getProduct().getSku())
                .quantity(i.getQuantity())
                .lastUpdated(i.getLastUpdated())
                .build();
    }
}
