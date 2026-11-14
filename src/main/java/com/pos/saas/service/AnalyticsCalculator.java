package com.pos.saas.service;

import com.pos.saas.domain.PaymentType;
import com.pos.saas.domain.UserRole;
import com.pos.saas.model.Branch;
import com.pos.saas.model.Inventory;
import com.pos.saas.model.Product;
import com.pos.saas.model.ShiftReport;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.analytics.*;
import com.pos.saas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * All dashboard maths. Every method takes a storeId and an optional branchId (null = whole store)
 * and never checks permissions: the callers (BranchAnalyticsService / StoreAnalyticsService) do that.
 * Money: "gross" = order totals, "refunds" = refunds processed in the period (by refund date), "net" = gross - refunds.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsCalculator {

    static final int LOW_STOCK_THRESHOLD = 10;
    static final int LONG_SHIFT_HOURS = 12;
    static final int MAX_ALERTS = 100;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AnalyticsRepository repo;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ShiftReportRepository shiftReportRepository;

    /** Half-open time window [from, to). */
    public record Window(LocalDateTime from, LocalDateTime to) {
        /** The last N days including today. */
        public static Window lastDays(int days) {
            LocalDate today = LocalDate.now();
            return new Window(today.minusDays(days - 1L).atStartOfDay(), today.plusDays(1).atStartOfDay());
        }
        public static Window day(LocalDate d) {
            return new Window(d.atStartOfDay(), d.plusDays(1).atStartOfDay());
        }
    }

    public static int bounded(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " must be between " + min + " and " + max);
        }
        return value;
    }

    // ============================================================ totals

    public PeriodStatsDto stats(Long storeId, Long branchId, Window w) {
        Object[] o = repo.orderTotals(storeId, branchId, w.from(), w.to()).get(0);
        Object[] r = repo.refundTotals(storeId, branchId, w.from(), w.to()).get(0);

        long orders = num(o[0]);
        BigDecimal gross = money(o[1]);
        BigDecimal refunds = money(r[1]);
        BigDecimal avg = orders == 0 ? ZERO : gross.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        return new PeriodStatsDto(orders, gross, refunds, gross.subtract(refunds), avg, num(o[2]));
    }

    /** (current - previous) / previous * 100, or null when there is nothing to compare with. */
    public BigDecimal growthPercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return null;
        return current.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP);
    }

    // ============================================================ sales series (zero-filled for charts)

    public List<SalesPointDto> daily(Long storeId, Long branchId, int days) {
        Window w = Window.lastDays(days);
        List<String> labels = new ArrayList<>();
        for (LocalDate d = w.from().toLocalDate(); d.isBefore(w.to().toLocalDate()); d = d.plusDays(1)) {
            labels.add(d.toString());
        }
        return series(labels,
                repo.dailyOrders(storeId, branchId, w.from(), w.to()),
                repo.dailyRefunds(storeId, branchId, w.from(), w.to()));
    }

    public List<SalesPointDto> weekly(Long storeId, Long branchId, int weeks) {
        LocalDate firstMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weeks - 1L);
        LocalDateTime from = firstMonday.atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < weeks; i++) labels.add(firstMonday.plusWeeks(i).toString());
        return series(labels,
                repo.weeklyOrders(storeId, branchId, from, to),
                repo.weeklyRefunds(storeId, branchId, from, to));
    }

    public List<SalesPointDto> monthly(Long storeId, Long branchId, int months) {
        YearMonth first = YearMonth.now().minusMonths(months - 1L);
        LocalDateTime from = first.atDay(1).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < months; i++) labels.add(first.plusMonths(i).toString());   // "2026-11"
        return series(labels,
                repo.monthlyOrders(storeId, branchId, from, to),
                repo.monthlyRefunds(storeId, branchId, from, to));
    }

    private static class Acc {
        long orders;
        BigDecimal gross = ZERO;
        BigDecimal refunds = ZERO;
    }

    /** Puts DB rows into a fixed list of labels so days / weeks / months with no sales show as 0. */
    private List<SalesPointDto> series(List<String> labels, List<Object[]> orderRows, List<Object[]> refundRows) {
        Map<String, Acc> map = new LinkedHashMap<>();
        labels.forEach(l -> map.put(l, new Acc()));

        for (Object[] r : orderRows) {
            Acc a = map.get(String.valueOf(r[0]));
            if (a != null) { a.orders = num(r[1]); a.gross = money(r[2]); }
        }
        for (Object[] r : refundRows) {
            Acc a = map.get(String.valueOf(r[0]));
            if (a != null) a.refunds = money(r[2]);
        }
        return map.entrySet().stream()
                .map(e -> new SalesPointDto(e.getKey(), e.getValue().orders, e.getValue().gross,
                        e.getValue().refunds, e.getValue().gross.subtract(e.getValue().refunds)))
                .toList();
    }

    // ============================================================ rankings

    public List<TopProductStatDto> topProducts(Long storeId, Long branchId, Window w, int limit) {
        BigDecimal total = money(repo.itemsRevenue(storeId, branchId, w.from(), w.to()));
        return repo.topProducts(storeId, branchId, w.from(), w.to(), PageRequest.of(0, limit)).stream()
                .map(r -> new TopProductStatDto((Long) r[0], (String) r[1], (String) r[2],
                        num(r[3]), money(r[4]), percent(money(r[4]), total)))
                .toList();
    }

    public List<TopCashierDto> topCashiers(Long storeId, Long branchId, Window w, int limit) {
        return repo.topCashiers(storeId, branchId, w.from(), w.to(), PageRequest.of(0, limit)).stream()
                .map(r -> new TopCashierDto((Long) r[0], (String) r[1], num(r[2]), money(r[3])))
                .toList();
    }

    public List<CategorySalesDto> categorySales(Long storeId, Long branchId, Window w) {
        List<Object[]> rows = repo.productTotals(storeId, branchId, w.from(), w.to());
        if (rows.isEmpty()) return List.of();

        Set<Long> ids = rows.stream().map(r -> (Long) r[0]).collect(Collectors.toSet());
        Map<Long, String> categoryOf = new HashMap<>();
        for (Product p : productRepository.findAllById(ids)) {
            categoryOf.put(p.getId(), p.getCategory() != null ? p.getCategory().getName() : "Uncategorized");
        }

        Map<String, long[]> qty = new HashMap<>();
        Map<String, BigDecimal> revenue = new HashMap<>();
        BigDecimal total = ZERO;
        for (Object[] r : rows) {
            String category = categoryOf.getOrDefault((Long) r[0], "Uncategorized");   // product deleted -> Uncategorized
            qty.computeIfAbsent(category, k -> new long[1])[0] += num(r[1]);
            revenue.merge(category, money(r[2]), BigDecimal::add);
            total = total.add(money(r[2]));
        }

        BigDecimal grand = total;
        return revenue.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategorySalesDto(e.getKey(), qty.get(e.getKey())[0], e.getValue(), percent(e.getValue(), grand)))
                .toList();
    }

    public List<PaymentBreakdownDto> paymentBreakdown(Long storeId, Long branchId, Window w) {
        class P { long orders, refunds; BigDecimal sales = ZERO, refunded = ZERO; }
        Map<PaymentType, P> map = new EnumMap<>(PaymentType.class);

        for (Object[] r : repo.orderPaymentBreakdown(storeId, branchId, w.from(), w.to())) {
            P p = map.computeIfAbsent((PaymentType) r[0], k -> new P());
            p.orders = num(r[1]);
            p.sales = money(r[2]);
        }
        for (Object[] r : repo.refundPaymentBreakdown(storeId, branchId, w.from(), w.to())) {
            P p = map.computeIfAbsent((PaymentType) r[0], k -> new P());
            p.refunds = num(r[1]);
            p.refunded = money(r[2]);
        }

        BigDecimal totalSales = map.values().stream().map(p -> p.sales).reduce(ZERO, BigDecimal::add);
        return map.entrySet().stream()
                .map(e -> new PaymentBreakdownDto(e.getKey(), e.getValue().orders, e.getValue().sales,
                        e.getValue().refunds, e.getValue().refunded,
                        e.getValue().sales.subtract(e.getValue().refunded), percent(e.getValue().sales, totalSales)))
                .toList();
    }

    public List<BranchPerformanceDto> branchPerformance(Long storeId, Window w) {
        Map<Long, long[]> orders = new HashMap<>();
        Map<Long, BigDecimal> gross = new HashMap<>();
        Map<Long, BigDecimal> refunds = new HashMap<>();

        for (Object[] r : repo.branchOrderTotals(storeId, w.from(), w.to())) {
            orders.put((Long) r[0], new long[]{num(r[1])});
            gross.put((Long) r[0], money(r[2]));
        }
        for (Object[] r : repo.branchRefundTotals(storeId, w.from(), w.to())) {
            refunds.put((Long) r[0], money(r[1]));
        }

        // every branch is listed, even if it sold nothing
        return branchRepository.findByStoreId(storeId).stream()
                .map(b -> {
                    long n = orders.containsKey(b.getId()) ? orders.get(b.getId())[0] : 0;
                    BigDecimal g = gross.getOrDefault(b.getId(), ZERO);
                    BigDecimal ref = refunds.getOrDefault(b.getId(), ZERO);
                    BigDecimal avg = n == 0 ? ZERO : g.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
                    return new BranchPerformanceDto(b.getId(), b.getName(), n, g, ref, g.subtract(ref), avg);
                })
                .sorted(Comparator.comparing(BranchPerformanceDto::netSales).reversed())
                .toList();
    }

    // ============================================================ counts for the store overview

    public long branches(Long storeId)  { return branchRepository.countByStoreId(storeId); }
    public long products(Long storeId)  { return productRepository.countByStoreId(storeId); }
    public long customers(Long storeId) { return customerRepository.countByStoreId(storeId); }
    public long employees(Long storeId) {
        return userRepository.countByStoreIdAndRoleIn(storeId, List.of(UserRole.ROLE_BRANCH_MANAGER, UserRole.ROLE_CASHIER));
    }
    public long openShifts(Long storeId, Long branchId) {
        return branchId == null
                ? shiftReportRepository.countByStoreIdAndShiftEndIsNull(storeId)
                : shiftReportRepository.countByBranchIdAndShiftEndIsNull(branchId);
    }

    // ============================================================ alerts

    public List<AlertDto> alerts(Long storeId, Long branchId) {
        Map<Long, String> branchName = branchRepository.findByStoreId(storeId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));
        List<AlertDto> out = new ArrayList<>();

        // stock
        List<Inventory> lowStock = branchId == null
                ? inventoryRepository.findByStoreIdAndQuantityLessThanEqual(storeId, LOW_STOCK_THRESHOLD)
                : inventoryRepository.findByBranchIdAndQuantityLessThanEqual(branchId, LOW_STOCK_THRESHOLD);
        for (Inventory i : lowStock) {
            String where = branchName.getOrDefault(i.getBranchId(), "branch " + i.getBranchId());
            String name = i.getProduct().getName();
            if (i.getQuantity() == 0) {
                out.add(new AlertDto("OUT_OF_STOCK", "CRITICAL", name + " is out of stock at " + where,
                        i.getBranchId(), i.getProduct().getId()));
            } else {
                out.add(new AlertDto("LOW_STOCK", "WARNING", name + " is running low at " + where + " (" + i.getQuantity() + " left)",
                        i.getBranchId(), i.getProduct().getId()));
            }
        }

        // shifts that have been open for too long (probably forgotten)
        LocalDateTime now = LocalDateTime.now();
        for (ShiftReport s : shiftReportRepository.findByStoreIdAndShiftEndIsNullAndShiftStartBefore(
                storeId, now.minusHours(LONG_SHIFT_HOURS))) {
            if (branchId != null && !branchId.equals(s.getBranchId())) continue;
            long hours = Duration.between(s.getShiftStart(), now).toHours();
            out.add(new AlertDto("LONG_OPEN_SHIFT", "WARNING",
                    s.getCashier().getFullName() + "'s shift has been open for " + hours + " hours",
                    s.getBranchId(), s.getId()));
        }

        // inactive employees
        List<User> staff = branchId == null ? userRepository.findByStoreId(storeId) : userRepository.findByBranchId(branchId);
        for (User u : staff) {
            boolean employee = u.getRole() == UserRole.ROLE_CASHIER || u.getRole() == UserRole.ROLE_BRANCH_MANAGER;
            if (employee && !u.isActive()) {
                out.add(new AlertDto("INACTIVE_EMPLOYEE", "INFO",
                        u.getFullName() + " (" + u.getRole().name().replace("ROLE_", "").toLowerCase() + ") is inactive",
                        u.getBranchId(), u.getId()));
            }
        }

        out.sort(Comparator.comparingInt((AlertDto a) -> severityRank(a.severity())).thenComparing(AlertDto::message));
        return out.size() > MAX_ALERTS ? out.subList(0, MAX_ALERTS) : out;
    }

    // ============================================================ helpers

    private int severityRank(String s) {
        return switch (s) { case "CRITICAL" -> 0; case "WARNING" -> 1; default -> 2; };
    }

    private BigDecimal percent(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) return BigDecimal.ZERO.setScale(1);
        return part.multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP);
    }

    static long num(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    static BigDecimal money(Object o) {
        if (o == null) return ZERO;
        BigDecimal v = o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
