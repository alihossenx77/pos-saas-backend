package com.pos.saas.repository;

import com.pos.saas.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    List<User> findByStoreId(Long storeId);
    List<User> findByBranchId(Long branchId);
    boolean existsByBranchId(Long branchId);

    /** Does the store have any user other than the given one (the store admin)? */
    boolean existsByStoreIdAndIdNot(Long storeId, Long id);

    /** Locks the user row (used to serialise "start shift" requests of the same cashier). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
