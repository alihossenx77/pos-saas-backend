package com.pos.saas.service;

import com.pos.saas.model.Inventory;
import com.pos.saas.model.Order;
import com.pos.saas.model.OrderItem;
import com.pos.saas.repository.InventoryRepository;
import com.pos.saas.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    /**
     * Puts an order's quantities back into its branch's stock.
     * Must run inside the caller's transaction. Rows are locked in productId order
     * (same order as when selling) so concurrent requests can't deadlock.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void restock(Order order) {
        order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductId))
                .forEach(item -> {
                    Optional<Inventory> existing = inventoryRepository.findForUpdate(order.getBranchId(), item.getProductId());
                    if (existing.isPresent()) {
                        Inventory inv = existing.get();
                        inv.setQuantity(inv.getQuantity() + item.getQuantity());
                        inventoryRepository.save(inv);
                    } else {
                        // stock row was deleted meanwhile: recreate it if the product still exists
                        productRepository.findByIdAndStoreId(item.getProductId(), order.getStoreId())
                                .ifPresent(p -> inventoryRepository.save(Inventory.builder()
                                        .branchId(order.getBranchId())
                                        .product(p)
                                        .quantity(item.getQuantity())
                                        .storeId(order.getStoreId())
                                        .build()));
                    }
                });
    }
}
