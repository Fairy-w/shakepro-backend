package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_materials", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_materials_user_barcode", columnNames = {"user_id", "barcode"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 120)
    private String brand;

    @Column(name = "category_id", length = 64)
    private String categoryId;

    @Column(nullable = false, length = 32)
    private String barcode;

    @Column(name = "capacity_text", length = 64)
    private String capacityText;

    @Column(name = "remain_level", nullable = false, length = 32)
    private String remainLevel;

    @Column(nullable = false)
    private Boolean opened;

    @Column(name = "has_item", nullable = false)
    private Boolean hasItem;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
