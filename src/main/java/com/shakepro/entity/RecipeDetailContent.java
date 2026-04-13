package com.shakepro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "recipe_detail_contents",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recipe_detail_structured_record", columnNames = {"structured_record_id"}),
                @UniqueConstraint(name = "uk_recipe_detail_recipe_key", columnNames = {"recipe_key"})
        },
        indexes = {
                @Index(name = "idx_recipe_detail_status", columnList = "status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDetailContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "structured_record_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_recipe_detail_structured")
    )
    private RecipeStructuredRecord structuredRecord;

    @Column(name = "recipe_key", nullable = false, length = 64)
    private String recipeKey;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "english_name", length = 100)
    private String englishName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "hero_image", nullable = false, length = 255)
    private String heroImage;

    @Column(length = 100)
    private String highlight;

    @Column(nullable = false, length = 150)
    private String subtitle;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 255)
    private String story;

    @Column(name = "best_for", nullable = false, length = 100)
    private String bestFor;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(nullable = false, length = 20)
    private String duration;

    @Column(nullable = false, length = 20)
    private String abv;

    @Column(nullable = false, length = 20)
    private String volume;

    @Column(nullable = false, length = 100)
    private String glass;

    @Column(nullable = false, length = 100)
    private String garnish;

    @Column(name = "serve_temperature", nullable = false, length = 50)
    private String serveTemperature;

    @Column(name = "flavor_tags_json", nullable = false, columnDefinition = "TEXT")
    private String flavorTagsJson;

    @Column(name = "flavor_metrics_json", nullable = false, columnDefinition = "TEXT")
    private String flavorMetricsJson;

    @Column(name = "pairings_json", columnDefinition = "TEXT")
    private String pairingsJson;

    @Column(name = "service_notes_json", columnDefinition = "TEXT")
    private String serviceNotesJson;

    @Column(name = "ingredients_json", nullable = false, columnDefinition = "LONGTEXT")
    private String ingredientsJson;

    @Column(name = "steps_json", nullable = false, columnDefinition = "LONGTEXT")
    private String stepsJson;

    @Column(name = "source_site", length = 50)
    private String sourceSite;

    @Column(name = "source_url", length = 255)
    private String sourceUrl;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "待审核";

    @Column(name = "ai_generated_at")
    private LocalDateTime aiGeneratedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
