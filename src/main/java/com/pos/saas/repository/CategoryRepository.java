package com.pos.saas.repository;

import com.pos.saas.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByStoreId(Long storeId);
    Optional<Category> findByIdAndStoreId(Long id, Long storeId);
    boolean existsByStoreId(Long storeId);
    boolean existsByStoreIdAndNameIgnoreCase(Long storeId, String name);
    boolean existsByStoreIdAndNameIgnoreCaseAndIdNot(Long storeId, String name, Long id);
}
