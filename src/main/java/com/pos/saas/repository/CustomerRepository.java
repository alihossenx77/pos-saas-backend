package com.pos.saas.repository;

import com.pos.saas.model.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndStoreId(Long id, Long storeId);
    boolean existsByStoreId(Long storeId);
    boolean existsByStoreIdAndPhone(Long storeId, String phone);
    boolean existsByStoreIdAndPhoneAndIdNot(Long storeId, String phone, Long id);

    List<Customer> findByStoreIdOrderByFullNameAsc(Long storeId, Pageable pageable);

    @Query("""
           select c from Customer c
           where c.storeId = :storeId
             and (lower(c.fullName) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.phone, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%')))
           order by c.fullName asc
           """)
    List<Customer> search(@Param("storeId") Long storeId, @Param("q") String q, Pageable pageable);

    long countByStoreId(Long storeId);
}
