package com.pos.saas.repository;

import com.pos.saas.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByStoreId(Long storeId);
    boolean existsByStoreId(Long storeId);
    boolean existsByStoreIdAndNameIgnoreCase(Long storeId, String name);
    boolean existsByStoreIdAndNameIgnoreCaseAndIdNot(Long storeId, String name, Long id);
}
