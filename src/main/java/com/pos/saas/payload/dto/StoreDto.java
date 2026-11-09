package com.pos.saas.payload.dto;

import com.pos.saas.domain.StoreStatus;
import com.pos.saas.model.Store;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class StoreDto {
    private Long id;
    private String brand;
    private String description;
    private String storeType;
    private String address;
    private String phone;
    private String email;
    private StoreStatus status;
    private Long storeAdminId;
    private String storeAdminName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StoreDto from(Store s) {
        return StoreDto.builder()
                .id(s.getId()).brand(s.getBrand()).description(s.getDescription())
                .storeType(s.getStoreType()).address(s.getAddress())
                .phone(s.getPhone()).email(s.getEmail())
                .status(s.getStatus())
                .storeAdminId(s.getStoreAdmin().getId())
                .storeAdminName(s.getStoreAdmin().getFullName())
                .createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt())
                .build();
    }
}
