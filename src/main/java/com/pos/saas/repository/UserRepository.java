package com.pos.saas.repository;

import com.pos.saas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    List<User> findByStoreId(Long storeId);
    List<User> findByBranchId(Long branchId);
    boolean existsByStoreId(Long storeId);
    boolean existsByBranchId(Long branchId);

    boolean existsByIdAndStoreId(Long id, Long id1);
}