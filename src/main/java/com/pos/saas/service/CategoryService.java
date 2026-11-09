package com.pos.saas.service;

import com.pos.saas.model.Category;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.CategoryDto;
import com.pos.saas.payload.request.CategoryRequest;
import com.pos.saas.repository.CategoryRepository;
import com.pos.saas.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AccessService access;

    @Transactional
    public CategoryDto create(CategoryRequest req) {
        User admin = access.currentStoreUser();
        String name = req.getName().trim();

        if (categoryRepository.existsByStoreIdAndNameIgnoreCase(admin.getStoreId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This category already exists");
        }
        return CategoryDto.from(categoryRepository.save(
                Category.builder().name(name).storeId(admin.getStoreId()).build()));
    }

    public List<CategoryDto> listByStore(Long storeId) {
        User u = access.currentStoreUser();
        if (!Objects.equals(u.getStoreId(), storeId)) throw AccessService.notFound("Store not found");
        return categoryRepository.findByStoreId(storeId).stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest req) {
        Category c = find(id);
        String name = req.getName().trim();

        if (categoryRepository.existsByStoreIdAndNameIgnoreCaseAndIdNot(c.getStoreId(), name, c.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This category already exists");
        }
        c.setName(name);
        return CategoryDto.from(categoryRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        Category c = find(id);
        if (productRepository.existsByCategoryId(c.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a category that still has products");
        }
        categoryRepository.delete(c);
    }

    private Category find(Long id) {
        User u = access.currentStoreUser();
        return categoryRepository.findByIdAndStoreId(id, u.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Category not found"));
    }
}
