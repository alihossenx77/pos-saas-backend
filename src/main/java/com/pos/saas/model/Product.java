package com.pos.saas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products",
       uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "sku"}),
       indexes = @Index(columnList = "store_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Unique per store. Also used as the barcode for the cashier's scanner / search. */
    @Column(nullable = false, length = 100)
    private String sku;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    private String brand;

    @Column(length = 500)
    private String image;      // Cloudinary URL later

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
