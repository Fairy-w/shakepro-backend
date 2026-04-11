package com.shakepro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorite_ai_cocktails",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_favorite_ai_cocktail_user_recipe", columnNames = {"user_id", "recipe_key"})
        },
        indexes = {
                @Index(name = "idx_favorite_ai_cocktail_user_created_at", columnList = "user_id, created_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteAiCocktail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipe_key", nullable = false, length = 128)
    private String recipeKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "materials_json", nullable = false, columnDefinition = "TEXT")
    private String materialsJson;

    @Column(name = "steps_json", nullable = false, columnDefinition = "TEXT")
    private String stepsJson;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false, length = 32)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
