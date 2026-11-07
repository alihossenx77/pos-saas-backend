package com.pos.saas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A Store is a tenant. Tenant-owned tables (added in later parts) carry its id as tenant_id. */
@Entity
@Table(name = "stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    private String description;

    @Column(nullable = false)
    private String status;   // ACTIVE, PENDING, BLOCKED

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}
