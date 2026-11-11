package com.pos.saas.service;

import com.pos.saas.domain.DiscountType;
import com.pos.saas.domain.OrderStatus;
import com.pos.saas.domain.PaymentType;
import com.pos.saas.domain.UserRole;
import com.pos.saas.model.*;
import com.pos.saas.payload.dto.OrderDto;
import com.pos.saas.payload.request.OrderItemRequest;
import com.pos.saas.payload.request.OrderRequest;
import com.pos.saas.repository.*;
import com.pos.saas.util.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final RefundRepository refundRepository;
    private final StockService stockService;
    private final ShiftReportRepository shiftReportRepository;
    private final ShiftReportService shiftReportService;
    private final AccessService access;

    /** app.shifts.require-open-shift=true -> a cashier must start a shift before selling. */
    @Value("${app.shifts.require-open-shift:true}")
    private boolean requireOpenShift;

    // ------------------------------------------------------------------ create (the sale)

    /**
     * One transaction: check stock (with row locks), take stock, price the items from the DB,
     * apply the discount, save the order. If anything fails, nothing is saved or deducted.
     */
    @Transactional
    public OrderDto create(OrderRequest req) {
        User cashier = access.currentStoreUser();
        if (cashier.getBranchId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are not assigned to a branch");
        }
        if (requireOpenShift && !shiftReportRepository.existsByCashierIdAndShiftEndIsNull(cashier.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Start your shift before making sales");
        }
        Long storeId = cashier.getStoreId();
        Long branchId = cashier.getBranchId();

        Customer customer = null;
        if (req.getCustomerId() != null) {
            customer = customerRepository.findByIdAndStoreId(req.getCustomerId(), storeId)
                    .orElseThrow(() -> AccessService.notFound("Customer not found"));
        }

        // merge repeated products; TreeMap = always lock stock rows in productId order (no deadlocks)
        Map<Long, Integer> qtyByProduct = new TreeMap<>();
        for (OrderItemRequest item : req.getItems()) {
            qtyByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        Order order = Order.builder()
                .storeId(storeId)
                .branchId(branchId)
                .cashier(cashier)
                .customer(customer)
                .paymentType(req.getPaymentType())
                .notes(req.getNotes())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> e : qtyByProduct.entrySet()) {
            Product product = productRepository.findByIdAndStoreId(e.getKey(), storeId)
                    .orElseThrow(() -> AccessService.notFound("Product " + e.getKey() + " not found"));

            Inventory stock = inventoryRepository.findForUpdate(branchId, product.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "'" + product.getName() + "' is not stocked in this branch"));

            int qty = e.getValue();
            if (stock.getQuantity() < qty) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for '" + product.getName() + "' (available: " + stock.getQuantity() + ")");
            }
            stock.setQuantity(stock.getQuantity() - qty);
            inventoryRepository.save(stock);

            BigDecimal unitPrice = product.getSellingPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            subtotal = subtotal.add(lineTotal);

            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getName())
                    .sku(product.getSku())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build());
        }

        BigDecimal discount = calculateDiscount(req.getDiscountType(), req.getDiscountValue(), subtotal);
        order.setSubtotal(subtotal);
        order.setDiscountType(req.getDiscountType());
        order.setDiscountValue(req.getDiscountValue());
        order.setDiscountAmount(discount);
        order.setTotalAmount(subtotal.subtract(discount));

        return OrderDto.from(orderRepository.save(order));
    }

    // ------------------------------------------------------------------ reads

    public OrderDto get(Long id) {
        return OrderDto.from(findAccessible(id));
    }

    public List<OrderDto> listByBranch(Long branchId, Long customerId, Long cashierId,
                                       PaymentType paymentType, OrderStatus status, int page, int size) {
        access.accessibleBranch(branchId);
        return orderRepository.filterByBranch(branchId, customerId, cashierId, paymentType, status, Paging.of(page, size))
                .stream().map(OrderDto::from).toList();
    }

    public List<OrderDto> listByCashier(Long cashierId, int page, int size) {
        User target = access.accessibleEmployee(cashierId);
        return orderRepository.findByCashierIdAndStoreIdOrderByCreatedAtDesc(
                        target.getId(), target.getStoreId(), Paging.of(page, size))
                .stream().map(OrderDto::from).toList();
    }

    public List<OrderDto> today(Long branchId) {
        access.accessibleBranch(branchId);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return orderRepository.findByBranchIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        branchId, start, start.plusDays(1))
                .stream().map(OrderDto::from).toList();
    }

    public List<OrderDto> recent(Long branchId) {
        access.accessibleBranch(branchId);
        return orderRepository.findTop5ByBranchIdOrderByCreatedAtDesc(branchId)
                .stream().map(OrderDto::from).toList();
    }

    /** Store admin sees the customer's orders in every branch; branch staff only their own branch. */
    public List<OrderDto> byCustomer(Long customerId, int page, int size) {
        User actor = access.currentStoreUser();
        customerRepository.findByIdAndStoreId(customerId, actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Customer not found"));

        Long branchScope = actor.getRole() == UserRole.ROLE_STORE_ADMIN ? null : actor.getBranchId();
        return orderRepository.findByCustomer(customerId, actor.getStoreId(), branchScope, Paging.of(page, size))
                .stream().map(OrderDto::from).toList();
    }

    // ------------------------------------------------------------------ delete (store admin only)

    /** Removes a mistaken sale entirely and puts its stock back. Orders that have refunds can't be deleted. */
    @Transactional
    public void delete(Long id) {
        User actor = access.currentStoreUser();
        Order order = orderRepository.findForUpdate(id, actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Order not found"));

        if (order.getRefundedAmount().signum() > 0 || refundRepository.existsByOrderId(order.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An order with refunds cannot be deleted");
        }
        stockService.restock(order);
        orderRepository.delete(order);
    }

    // ------------------------------------------------------------------ helpers

    private Order findAccessible(Long id) {
        User actor = access.currentStoreUser();
        Order order = orderRepository.findByIdAndStoreId(id, actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Order not found"));
        if (actor.getRole() != UserRole.ROLE_STORE_ADMIN && !Objects.equals(actor.getBranchId(), order.getBranchId())) {
            throw AccessService.notFound("Order not found");
        }
        return order;
    }

    private BigDecimal calculateDiscount(DiscountType type, BigDecimal value, BigDecimal subtotal) {
        if (type == null && value == null) return BigDecimal.ZERO.setScale(2);
        if (type == null || value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "discountType and discountValue must be sent together");
        }

        BigDecimal discount;
        if (type == DiscountType.PERCENTAGE) {
            if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
            }
            discount = subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            if (value.compareTo(subtotal) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount cannot be more than the order subtotal");
            }
            discount = value.setScale(2, RoundingMode.HALF_UP);
        }
        return discount;
    }
}
