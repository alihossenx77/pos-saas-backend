package com.pos.saas.repository;

import com.pos.saas.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndStoreId(Long id, Long storeId);
    List<Product> findByStoreId(Long storeId);
    boolean existsByStoreId(Long storeId);
    boolean existsByStoreIdAndSku(Long storeId, String sku);
    boolean existsByCategoryId(Long categoryId);

    @Query("""
           select p from Product p
           where p.storeId = :storeId
             and (lower(p.name) like lower(concat('%', :q, '%'))
               or lower(p.sku)  like lower(concat('%', :q, '%'))
               or lower(coalesce(p.brand, '')) like lower(concat('%', :q, '%')))
           """)
    List<Product> search(@Param("storeId") Long storeId, @Param("q") String q);
}
