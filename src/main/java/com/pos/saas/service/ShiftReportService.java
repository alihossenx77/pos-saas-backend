package com.pos.saas.service;

import com.pos.saas.domain.PaymentType;
import com.pos.saas.model.ShiftReport;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.*;
import com.pos.saas.repository.OrderRepository;
import com.pos.saas.repository.RefundRepository;
import com.pos.saas.repository.ShiftReportRepository;
import com.pos.saas.repository.UserRepository;
import com.pos.saas.util.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.pos.saas.domain.UserRole;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A cashier starts a shift, sells, processes refunds, then ends the shift.
 * Orders / refunds belong to the shift by TIME (created by that cashier between start and end).
 */
@Service
@RequiredArgsConstructor
public class ShiftReportService {

    private final ShiftReportRepository shiftRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final AccessService access;

    /** true (default) = a cashier must have an open shift to sell or refund. */
    @Value("${app.shifts.require-open-shift:true}")
    private boolean requireOpenShift;

    /**
     * Cashiers can only sell / refund inside an open shift.
     * Store admins and branch managers don't work in shifts, so they are never blocked.
     */
    public void assertShiftOpenForCashier(User actor) {
        if (!requireOpenShift || actor.getRole() != UserRole.ROLE_CASHIER) return;
        if (!shiftRepository.existsByCashierIdAndShiftEndIsNull(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Start your shift first (POST /api/shift-reports/start)");
        }
    }

    // ============================================================ cashier actions

    @Transactional
    public ShiftReportDto start() {
        User cashier = access.currentStoreUser();
        if (cashier.getBranchId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are not assigned to a branch");
        }

        // lock this cashier's row: two "start" requests can't both pass the check below
        userRepository.findByIdForUpdate(cashier.getId());
        if (shiftRepository.existsByCashierIdAndShiftEndIsNull(cashier.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an open shift. End it first.");
        }

        ShiftReport shift = shiftRepository.save(ShiftReport.builder()
                .storeId(cashier.getStoreId())
                .branchId(cashier.getBranchId())
                .cashier(cashier)
                .shiftStart(LocalDateTime.now())
                .build());
        return toDto(shift, true);
    }

    @Transactional
    public ShiftReportDto end() {
        User cashier = access.currentStoreUser();
        ShiftReport shift = shiftRepository.findOpenForUpdate(cashier.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "You have no open shift"));
        return toDto(close(shift), true);
    }

    /** Live progress of the cashier's running shift. */
    public ShiftReportDto current() {
        User cashier = access.currentStoreUser();
        ShiftReport shift = shiftRepository.findFirstByCashierIdAndShiftEndIsNull(cashier.getId())
                .orElseThrow(() -> AccessService.notFound("You have no open shift"));
        return toDto(shift, true);
    }

    // ============================================================ manager / admin action

    /** Closes a forgotten shift (store admin: any, branch manager: own branch). */
    @Transactional
    public ShiftReportDto forceClose(Long id) {
        User actor = access.currentStoreUser();
        ShiftReport shift = shiftRepository.findForUpdate(id, actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Shift report not found"));
        assertCanView(actor, shift);

        if (shift.getShiftEnd() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This shift is already closed");
        }
        return toDto(close(shift), true);
    }

    // ============================================================ reads

    public ShiftReportDto get(Long id) {
        return toDto(findAccessible(id), true);
    }

    public List<ShiftReportDto> byCashier(Long cashierId, int page, int size) {
        User target = access.accessibleEmployee(cashierId);
        return shiftRepository.findByCashierIdAndStoreIdOrderByShiftStartDesc(
                        target.getId(), target.getStoreId(), Paging.of(page, size))
                .stream().map(s -> toDto(s, false)).toList();
    }

    /** All shifts a cashier STARTED on that date (there can be more than one a day), with full details. */
    public List<ShiftReportDto> byCashierAndDate(Long cashierId, LocalDate date) {
        User target = access.accessibleEmployee(cashierId);
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay().minusNanos(1);
        return shiftRepository.findByCashierIdAndStoreIdAndShiftStartBetweenOrderByShiftStartDesc(
                        target.getId(), target.getStoreId(), from, to)
                .stream().map(s -> toDto(s, true)).toList();
    }

    public List<ShiftReportDto> byBranch(Long branchId, int page, int size) {
        access.accessibleBranch(branchId);
        return shiftRepository.findByBranchIdOrderByShiftStartDesc(branchId, Paging.of(page, size))
                .stream().map(s -> toDto(s, false)).toList();
    }

    public List<ShiftReportDto> all(int page, int size) {
        User actor = access.currentStoreUser();
        return shiftRepository.findByStoreIdOrderByShiftStartDesc(actor.getStoreId(), Paging.of(page, size))
                .stream().map(s -> toDto(s, false)).toList();
    }

    @Transactional
    public void delete(Long id) {
        User actor = access.currentStoreUser();
        ShiftReport shift = shiftRepository.findForUpdate(id, actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Shift report not found"));
        if (shift.getShiftEnd() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Close the shift before deleting its report");
        }
        shiftRepository.delete(shift);
    }

    /** Used by RefundService: the shift if the caller is allowed to see it. */
    public ShiftReport findAccessible(Long id) {
        User actor = access.currentStoreUser();
        ShiftReport shift = shiftRepository.findByIdAndStoreId(id, actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Shift report not found"));
        assertCanView(actor, shift);
        return shift;
    }

    // ============================================================ internals

    /** admin: any shift of the store; manager: own branch; cashier: own shifts. */
    private void assertCanView(User actor, ShiftReport shift) {
        boolean ok = switch (actor.getRole()) {
            case ROLE_STORE_ADMIN -> true;
            case ROLE_BRANCH_MANAGER -> Objects.equals(actor.getBranchId(), shift.getBranchId());
            case ROLE_CASHIER -> actor.getId().equals(shift.getCashier().getId());
            default -> false;
        };
        if (!ok) throw AccessService.notFound("Shift report not found");
    }

    /** Ends the shift now and saves its totals. */
    private ShiftReport close(ShiftReport shift) {
        LocalDateTime end = LocalDateTime.now();
        Totals t = totals(shift.getCashier().getId(), shift.getShiftStart(), end);

        shift.setShiftEnd(end);
        shift.setTotalOrders(t.orders());
        shift.setTotalSales(t.sales());
        shift.setTotalRefunds(t.refunds());
        shift.setNetSales(t.sales().subtract(t.refunds()));
        return shiftRepository.save(shift);
    }

    private record Totals(int orders, BigDecimal sales, BigDecimal refunds) {}

    private Totals totals(Long cashierId, LocalDateTime from, LocalDateTime to) {
        long orders = orderRepository.countByCashierIdAndCreatedAtBetween(cashierId, from, to);
        BigDecimal sales = nz(orderRepository.sumTotalByCashier(cashierId, from, to));
        BigDecimal refunds = nz(refundRepository.sumAmountByCashier(cashierId, from, to));
        return new Totals((int) orders, sales, refunds);
    }

    private ShiftReportDto toDto(ShiftReport s, boolean details) {
        boolean open = s.getShiftEnd() == null;
        LocalDateTime to = open ? LocalDateTime.now() : s.getShiftEnd();
        Long cashierId = s.getCashier().getId();

        int orders;
        BigDecimal sales, refunds;
        if (open) {                               // live numbers
            Totals t = totals(cashierId, s.getShiftStart(), to);
            orders = t.orders();
            sales = t.sales();
            refunds = t.refunds();
        } else {                                  // numbers saved when the shift was closed
            orders = s.getTotalOrders() == null ? 0 : s.getTotalOrders();
            sales = nz(s.getTotalSales());
            refunds = nz(s.getTotalRefunds());
        }

        var dto = ShiftReportDto.builder()
                .id(s.getId()).storeId(s.getStoreId()).branchId(s.getBranchId())
                .cashierId(cashierId).cashierName(s.getCashier().getFullName())
                .shiftStart(s.getShiftStart()).shiftEnd(s.getShiftEnd())
                .status(open ? "OPEN" : "CLOSED")
                .totalOrders(orders).totalSales(sales).totalRefunds(refunds)
                .netSales(sales.subtract(refunds));

        if (details) {
            dto.paymentSummaries(paymentSummaries(cashierId, s.getShiftStart(), to))
                    .topSellingProducts(topProducts(cashierId, s.getShiftStart(), to))
                    .recentOrders(orderRepository.findTop5ByCashierIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                            cashierId, s.getShiftStart(), to).stream().map(OrderDto::from).toList())
                    .refunds(refundRepository.findByCashierIdAndStoreIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                            cashierId, s.getStoreId(), s.getShiftStart(), to).stream().map(RefundDto::from).toList());
        }
        return dto.build();
    }

    private static class Acc {
        long orders, refunds;
        BigDecimal sales = BigDecimal.ZERO.setScale(2);
        BigDecimal refundAmount = BigDecimal.ZERO.setScale(2);
    }

    private List<PaymentSummaryDto> paymentSummaries(Long cashierId, LocalDateTime from, LocalDateTime to) {
        Map<PaymentType, Acc> map = new EnumMap<>(PaymentType.class);

        for (Object[] r : orderRepository.paymentBreakdown(cashierId, from, to)) {
            Acc a = map.computeIfAbsent((PaymentType) r[0], k -> new Acc());
            a.orders = ((Number) r[1]).longValue();
            a.sales = (BigDecimal) r[2];
        }
        for (Object[] r : refundRepository.paymentBreakdown(cashierId, from, to)) {
            Acc a = map.computeIfAbsent((PaymentType) r[0], k -> new Acc());
            a.refunds = ((Number) r[1]).longValue();
            a.refundAmount = (BigDecimal) r[2];
        }

        return map.entrySet().stream()
                .map(e -> PaymentSummaryDto.builder()
                        .paymentType(e.getKey())
                        .orderCount(e.getValue().orders)
                        .salesAmount(e.getValue().sales)
                        .refundCount(e.getValue().refunds)
                        .refundAmount(e.getValue().refundAmount)
                        .netAmount(e.getValue().sales.subtract(e.getValue().refundAmount))
                        .build())
                .toList();
    }

    private List<TopProductDto> topProducts(Long cashierId, LocalDateTime from, LocalDateTime to) {
        return orderRepository.topProducts(cashierId, from, to, PageRequest.of(0, 5)).stream()
                .map(r -> TopProductDto.builder()
                        .productId((Long) r[0])
                        .productName((String) r[1])
                        .sku((String) r[2])
                        .quantitySold(((Number) r[3]).longValue())
                        .grossRevenue((BigDecimal) r[4])
                        .build())
                .toList();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v;
    }
}
