package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "barcode_product_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarcodeProductCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String barcode;

    @Column(name = "product_key", nullable = false, length = 128)
    private String productKey;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "source_label", length = 64)
    private String sourceLabel;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String subtitle;

    @Column(length = 120)
    private String brand;

    @Column(name = "category_id", length = 64)
    private String categoryId;

    @Column(name = "capacity_text", length = 64)
    private String capacityText;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @Column(length = 500)
    private String note;

    @Column(length = 16)
    private String badge;

    @Column(name = "accent_color", length = 16)
    private String accentColor;

    @Column(name = "soft_color", length = 16)
    private String softColor;

    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
