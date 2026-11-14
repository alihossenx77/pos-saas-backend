package com.pos.saas.service;

import com.pos.saas.domain.StoreStatus;
import com.pos.saas.payload.dto.analytics.DailyCountDto;
import com.pos.saas.payload.dto.analytics.StatusCountDto;
import com.pos.saas.payload.dto.analytics.SuperAdminSummaryDto;
import com.pos.saas.repository.AnalyticsRepository;
import com.pos.saas.repository.StoreRepository;
import com.pos.saas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/** Platform-wide numbers. The controller is restricted to ROLE_SUPER_ADMIN. */
@Service
@RequiredArgsConstructor
public class SuperAdminDashboardService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;

    public SuperAdminSummaryDto summary() {
        long approved = storeRepository.countByStatus(StoreStatus.APPROVED);
        long pending = storeRepository.countByStatus(StoreStatus.PENDING);
        long declined = storeRepository.countByStatus(StoreStatus.DECLINED);
        long blocked = storeRepository.countByStatus(StoreStatus.BLOCKED);
        return new SuperAdminSummaryDto(approved + pending + declined + blocked, approved, pending, declined, blocked,
                userRepository.count());
    }

    /** Stores registered per day for the last N days (zero-filled, for a line / bar chart). */
    public List<DailyCountDto> registrations(int days) {
        int n = AnalyticsCalculator.bounded(days, 1, 90, "days");
        LocalDate today = LocalDate.now();
        LocalDate first = today.minusDays(n - 1L);

        Map<String, Long> counts = new LinkedHashMap<>();
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) counts.put(d.toString(), 0L);

        for (Object[] r : analyticsRepository.storeRegistrationsDaily(first.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            counts.computeIfPresent(String.valueOf(r[0]), (k, v) -> ((Number) r[1]).longValue());
        }
        return counts.entrySet().stream().map(e -> new DailyCountDto(e.getKey(), e.getValue())).toList();
    }

    /** For a pie chart. */
    public List<StatusCountDto> statusDistribution() {
        return Arrays.stream(StoreStatus.values())
                .map(s -> new StatusCountDto(s.name(), storeRepository.countByStatus(s)))
                .toList();
    }
}
