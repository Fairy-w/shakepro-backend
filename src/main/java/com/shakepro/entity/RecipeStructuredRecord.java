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
        name = "recipe_structured_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recipe_structured_source_record", columnNames = {"source_record_id"}),
                @UniqueConstraint(name = "uk_recipe_structured_recipe_key", columnNames = {"recipe_key"})
        },
        indexes = {
                @Index(name = "idx_recipe_structured_status", columnList = "status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStructuredRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "source_record_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_recipe_structured_source")
    )
    private RecipeSourceRecord sourceRecord;

    @Column(name = "recipe_key", nullable = false, length = 64)
    private String recipeKey;

    @Column(name = "english_name", nullable = false, length = 100)
    private String englishName;

    @Column(name = "chinese_name_draft", length = 100)
    private String chineseNameDraft;

    @Column(length = 100)
    private String category;

    @Column(name = "hero_image", length = 255)
    private String heroImage;

    @Column(length = 100)
    private String garnish;

    @Column(name = "glass_draft", length = 100)
    private String glassDraft;

    @Column(name = "method_text", columnDefinition = "TEXT")
    private String methodText;

    @Column(name = "ingredients_json", nullable = false, columnDefinition = "LONGTEXT")
    private String ingredientsJson;

    @Column(name = "steps_json", nullable = false, columnDefinition = "LONGTEXT")
    private String stepsJson;

    @Column(name = "estimated_abv", length = 20)
    private String estimatedAbv;

    @Column(name = "estimated_volume", length = 20)
    private String estimatedVolume;

    @Column(name = "parse_notes", columnDefinition = "TEXT")
    private String parseNotes;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "已解析";

    @CreationTimestamp
    @Column(name = "parsed_at", nullable = false, updatable = false)
    private LocalDateTime parsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
