package com.pos.saas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Stock of ONE product in ONE branch. */
@Entity
@Table(name = "inventories",
       uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "product_id"}),
       indexes = @Index(columnList = "store_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    void touch() {
        lastUpdated = LocalDateTime.now();
    }
}
