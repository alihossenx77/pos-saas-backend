package com.pos.saas.model;

import com.pos.saas.domain.StoreStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Store names are NOT unique. Each store has exactly one store admin (OneToOne). */
@Entity
@Table(name = "stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(length = 500)
    private String description;

    private String storeType;

    @Column(nullable = false)
    private String address;

    private String phone;
    private String email;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status = StoreStatus.PENDING;

    /** One admin <-> one store. The unique FK enforces it in the database. */
    @OneToOne(optional = false)
    @JoinColumn(name = "store_admin_id", nullable = false, unique = true)
    private User storeAdminId;

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