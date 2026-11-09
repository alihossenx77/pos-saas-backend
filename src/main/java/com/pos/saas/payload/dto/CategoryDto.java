package com.pos.saas.payload.dto;

import com.pos.saas.model.Category;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CategoryDto {
    private Long id;
    private String name;
    private Long storeId;

    public static CategoryDto from(Category c) {
        return CategoryDto.builder().id(c.getId()).name(c.getName()).storeId(c.getStoreId()).build();
    }
}
