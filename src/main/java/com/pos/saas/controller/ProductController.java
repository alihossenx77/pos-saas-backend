package com.pos.saas.controller;

import com.pos.saas.payload.dto.ProductDto;
import com.pos.saas.payload.request.ProductRequest;
import com.pos.saas.payload.request.ProductUpdateRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<ProductDto> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.created("Product created", productService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDto> get(@PathVariable Long id) {
        return ApiResponse.ok(productService.get(id));
    }

    /** Spec uses PATCH; PUT is accepted too. Only the fields you send are changed. */
    @RequestMapping(value = "/{id}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<ProductDto> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.ok("Product updated", productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.message("Product deleted");
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<List<ProductDto>> byStore(@PathVariable Long storeId) {
        return ApiResponse.ok(productService.listByStore(storeId));
    }

    /** Cashier search box / barcode scan: matches name, SKU or brand. */
    @GetMapping("/store/{storeId}/search")
    public ApiResponse<List<ProductDto>> search(@PathVariable Long storeId,
                                                @RequestParam(name = "q", required = false) String q) {
        return ApiResponse.ok(productService.search(storeId, q));
    }
}
