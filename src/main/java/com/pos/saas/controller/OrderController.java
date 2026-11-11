package com.pos.saas.controller;

import com.pos.saas.domain.OrderStatus;
import com.pos.saas.domain.PaymentType;
import com.pos.saas.payload.dto.OrderDto;
import com.pos.saas.payload.request.OrderRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER','ROLE_CASHIER')")
public class OrderController {

    private final OrderService orderService;

    /** The sale: only cashiers. Branch = the cashier's branch, prices come from the products. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CASHIER')")
    public ApiResponse<OrderDto> create(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.created("Order created", orderService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDto> get(@PathVariable Long id) {
        return ApiResponse.ok(orderService.get(id));
    }

    /** GET /api/orders/branch/1?customerId=&cashierId=&paymentType=CASH&status=COMPLETED&page=0&size=50 */
    @GetMapping("/branch/{branchId}")
    public ApiResponse<List<OrderDto>> byBranch(@PathVariable Long branchId,
                                                @RequestParam(required = false) Long customerId,
                                                @RequestParam(required = false) Long cashierId,
                                                @RequestParam(required = false) PaymentType paymentType,
                                                @RequestParam(required = false) OrderStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(orderService.listByBranch(branchId, customerId, cashierId, paymentType, status, page, size));
    }

    @GetMapping("/cashier/{cashierId}")
    public ApiResponse<List<OrderDto>> byCashier(@PathVariable Long cashierId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(orderService.listByCashier(cashierId, page, size));
    }

    @GetMapping("/today/branch/{branchId}")
    public ApiResponse<List<OrderDto>> today(@PathVariable Long branchId) {
        return ApiResponse.ok(orderService.today(branchId));
    }

    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<OrderDto>> byCustomer(@PathVariable Long customerId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(orderService.byCustomer(customerId, page, size));
    }

    /** Top 5 most recent orders of a branch. */
    @GetMapping("/recent/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_BRANCH_MANAGER')")
    public ApiResponse<List<OrderDto>> recent(@PathVariable Long branchId) {
        return ApiResponse.ok(orderService.recent(branchId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ApiResponse.message("Order deleted and stock restored");
    }
}
