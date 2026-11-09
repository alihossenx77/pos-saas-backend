package com.pos.saas.repository;

import com.pos.saas.domain.StoreStatus;
import com.pos.saas.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByStoreAdminId(Long storeAdminId);
    boolean existsByStoreAdminId(Long storeAdminId);
    List<Store> findByStatus(StoreStatus status);
}
