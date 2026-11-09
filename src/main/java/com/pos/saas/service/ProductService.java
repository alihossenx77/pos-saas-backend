package com.pos.saas.service;

import com.pos.saas.model.Category;
import com.pos.saas.model.Product;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.ProductDto;
import com.pos.saas.payload.request.ProductRequest;
import com.pos.saas.payload.request.ProductUpdateRequest;
import com.pos.saas.repository.CategoryRepository;
import com.pos.saas.repository.InventoryRepository;
import com.pos.saas.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final AccessService access;

    @Transactional
    public ProductDto create(ProductRequest req) {
        User admin = access.currentStoreUser();
        Long storeId = admin.getStoreId();
        String sku = req.getSku().trim();

        if (productRepository.existsByStoreIdAndSku(storeId, sku)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this SKU already exists");
        }
        checkPrices(req.getMrp(), req.getSellingPrice());

        Product product = Product.builder()
                .name(req.getName().trim())
                .sku(sku)
                .description(req.getDescription())
                .mrp(req.getMrp())
                .sellingPrice(req.getSellingPrice())
                .brand(req.getBrand())
                .image(req.getImage())
                .category(resolveCategory(req.getCategoryId(), storeId))
                .storeId(storeId)
                .build();
        return ProductDto.from(productRepository.save(product));
    }

    public ProductDto get(Long id) {
        return ProductDto.from(find(id));
    }

    public List<ProductDto> listByStore(Long storeId) {
        assertOwnStore(storeId);
        return productRepository.findByStoreId(storeId).stream().map(ProductDto::from).toList();
    }

    /** Cashier search box / barcode scan: matches name, SKU or brand. */
    public List<ProductDto> search(Long storeId, String q) {
        assertOwnStore(storeId);
        if (q == null || q.isBlank()) return listByStore(storeId);
        return productRepository.search(storeId, q.trim()).stream().map(ProductDto::from).toList();
    }

    /** PATCH-style: only fields present in the request are changed. */
    @Transactional
    public ProductDto update(Long id, ProductUpdateRequest req) {
        Product p = find(id);

        if (req.getName() != null && !req.getName().isBlank()) p.setName(req.getName().trim());
        if (req.getSku() != null && !req.getSku().isBlank() && !req.getSku().trim().equals(p.getSku())) {
            String sku = req.getSku().trim();
            if (productRepository.existsByStoreIdAndSku(p.getStoreId(), sku)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this SKU already exists");
            }
            p.setSku(sku);
        }
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getBrand() != null) p.setBrand(req.getBrand());
        if (req.getImage() != null) p.setImage(req.getImage());
        if (req.getMrp() != null) p.setMrp(req.getMrp());
        if (req.getSellingPrice() != null) p.setSellingPrice(req.getSellingPrice());
        if (req.getCategoryId() != null) p.setCategory(resolveCategory(req.getCategoryId(), p.getStoreId()));

        checkPrices(p.getMrp(), p.getSellingPrice());
        return ProductDto.from(productRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        Product p = find(id);
        inventoryRepository.deleteByProductId(p.getId());   // remove its stock rows in all branches
        productRepository.delete(p);
    }

    // ---- helpers ----

    private Product find(Long id) {
        User u = access.currentStoreUser();
        return productRepository.findByIdAndStoreId(id, u.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Product not found"));
    }

    private void assertOwnStore(Long storeId) {
        User u = access.currentStoreUser();
        if (!Objects.equals(u.getStoreId(), storeId)) throw AccessService.notFound("Store not found");
    }

    private Category resolveCategory(Long categoryId, Long storeId) {
        if (categoryId == null) return null;
        return categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> AccessService.notFound("Category not found"));
    }

    private void checkPrices(BigDecimal mrp, BigDecimal selling) {
        if (selling.compareTo(mrp) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selling price cannot be higher than MRP");
        }
    }
}
