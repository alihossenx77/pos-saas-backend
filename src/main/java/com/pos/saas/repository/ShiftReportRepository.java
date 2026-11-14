package com.pos.saas.repository;

import com.pos.saas.model.ShiftReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftReportRepository extends JpaRepository<ShiftReport, Long> {

    Optional<ShiftReport> findByIdAndStoreId(Long id, Long storeId);
    Optional<ShiftReport> findFirstByCashierIdAndShiftEndIsNull(Long cashierId);
    boolean existsByCashierIdAndShiftEndIsNull(Long cashierId);
    boolean existsByCashierId(Long cashierId);

    /** Row lock: ending / force-closing the same shift twice can't both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShiftReport s where s.cashier.id = :cashierId and s.shiftEnd is null")
    Optional<ShiftReport> findOpenForUpdate(@Param("cashierId") Long cashierId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShiftReport s where s.id = :id and s.storeId = :storeId")
    Optional<ShiftReport> findForUpdate(@Param("id") Long id, @Param("storeId") Long storeId);

    List<ShiftReport> findByStoreIdOrderByShiftStartDesc(Long storeId, Pageable pageable);
    List<ShiftReport> findByBranchIdOrderByShiftStartDesc(Long branchId, Pageable pageable);
    List<ShiftReport> findByCashierIdAndStoreIdOrderByShiftStartDesc(Long cashierId, Long storeId, Pageable pageable);
    List<ShiftReport> findByCashierIdAndStoreIdAndShiftStartBetweenOrderByShiftStartDesc(
            Long cashierId, Long storeId, LocalDateTime from, LocalDateTime to);

    long countByBranchIdAndShiftEndIsNull(Long branchId);
    long countByStoreIdAndShiftEndIsNull(Long storeId);
    List<ShiftReport> findByStoreIdAndShiftEndIsNullAndShiftStartBefore(Long storeId, LocalDateTime before);
}
