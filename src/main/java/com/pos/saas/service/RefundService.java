package com.pos.saas.service;

import com.pos.saas.domain.OrderStatus;
import com.pos.saas.model.Order;
import com.pos.saas.model.Refund;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.RefundDto;
import com.pos.saas.payload.request.RefundRequest;
import com.pos.saas.repository.OrderRepository;
import com.pos.saas.repository.RefundRepository;
import com.pos.saas.util.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Refunds are an audit trail, so there is no delete.
 * A FULL refund of an order that had no earlier refunds also returns its items to stock.
 * Partial refunds are money only (we can't know which items came back).
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final StockService stockService;
    private final AccessService access;

    @Transactional
    public RefundDto create(RefundRequest req) {
        User actor = access.currentStoreUser();

        // lock the order so two refunds can't both pass the "remaining amount" check
        Order order = orderRepository.findForUpdate(req.getOrderId(), actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Order not found"));
        access.accessibleBranch(order.getBranchId());      // cashier / manager: only their own branch

        if (order.getTotalAmount().signum() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nothing to refund: the order total is 0");
        }
        BigDecimal remaining = order.getTotalAmount().subtract(order.getRefundedAmount());
        if (remaining.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is already fully refunded");
        }

        BigDecimal amount = req.getAmount() == null ? remaining : req.getAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Refund amount cannot exceed the remaining refundable amount (" + remaining + ")");
        }

        boolean wholeOrderAtOnce = order.getRefundedAmount().signum() == 0
                && amount.compareTo(order.getTotalAmount()) == 0;

        BigDecimal refundedTotal = order.getRefundedAmount().add(amount);
        order.setRefundedAmount(refundedTotal);
        order.setStatus(refundedTotal.compareTo(order.getTotalAmount()) == 0
                ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED);
        orderRepository.save(order);

        if (wholeOrderAtOnce) {
            stockService.restock(order);
        }

        Refund refund = refundRepository.save(Refund.builder()
                .orderId(order.getId())
                .storeId(order.getStoreId())
                .branchId(order.getBranchId())
                .cashier(actor)
                .amount(amount)
                .reason(req.getReason().trim())
                .paymentType(req.getPaymentType() != null ? req.getPaymentType() : order.getPaymentType())
                .build());
        return RefundDto.from(refund);
    }

    /** Store admin: every refund of the store. */
    public List<RefundDto> listAll(int page, int size) {
        User actor = access.currentStoreUser();
        return refundRepository.findByStoreIdOrderByCreatedAtDesc(actor.getStoreId(), Paging.of(page, size))
                .stream().map(RefundDto::from).toList();
    }

    public List<RefundDto> byBranch(Long branchId, int page, int size) {
        access.accessibleBranch(branchId);
        return refundRepository.findByBranchIdOrderByCreatedAtDesc(branchId, Paging.of(page, size))
                .stream().map(RefundDto::from).toList();
    }

    public List<RefundDto> byCashier(Long cashierId, int page, int size) {
        System.out.println("Cashier Id -------- > "+ cashierId);
        User target = access.accessibleEmployee(cashierId);
        return refundRepository.findByCashierIdAndStoreIdOrderByCreatedAtDesc(
                        target.getId(), target.getStoreId(), Paging.of(page, size))
                .stream().map(RefundDto::from).toList();
    }

    public List<RefundDto> byCashierInRange(Long cashierId, LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before 'to'");
        }
        User target = access.accessibleEmployee(cashierId);
        return refundRepository.findByCashierIdAndStoreIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        target.getId(), target.getStoreId(), from, to)
                .stream().map(RefundDto::from).toList();
    }

    public RefundDto get(Long id) {
        User actor = access.currentStoreUser();
        Refund refund = refundRepository.findByIdAndStoreId(id, actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Refund not found"));
        access.accessibleBranch(refund.getBranchId());
        return RefundDto.from(refund);
    }
}
