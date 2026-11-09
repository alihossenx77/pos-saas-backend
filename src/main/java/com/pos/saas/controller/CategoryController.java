package com.pos.saas.controller;

import com.pos.saas.payload.dto.CategoryDto;
import com.pos.saas.payload.request.CategoryRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<CategoryDto> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.created("Category created", categoryService.create(request));
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<List<CategoryDto>> byStore(@PathVariable Long storeId) {
        return ApiResponse.ok(categoryService.listByStore(storeId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<CategoryDto> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category updated", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.message("Category deleted");
    }
}
