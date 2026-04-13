package com.shakepro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "recipe_source_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recipe_source_url", columnNames = {"source_url"})
        },
        indexes = {
                @Index(name = "idx_recipe_source_site", columnList = "source_site"),
                @Index(name = "idx_recipe_source_status", columnList = "status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSourceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_site", nullable = false, length = 50)
    private String sourceSite;

    @Column(name = "source_url", nullable = false, length = 255)
    private String sourceUrl;

    @Column(name = "page_type", nullable = false, length = 20)
    @Builder.Default
    private String pageType = "detail";

    @Column(name = "raw_title", length = 255)
    private String rawTitle;

    @Column(name = "raw_category", length = 100)
    private String rawCategory;

    @Column(name = "raw_image_url", length = 255)
    private String rawImageUrl;

    @Column(name = "raw_ingredients_text", columnDefinition = "TEXT")
    private String rawIngredientsText;

    @Column(name = "raw_method_text", columnDefinition = "TEXT")
    private String rawMethodText;

    @Column(name = "raw_garnish_text", length = 255)
    private String rawGarnishText;

    @Column(name = "raw_html", columnDefinition = "LONGTEXT")
    private String rawHtml;

    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "已抓取";

    @CreationTimestamp
    @Column(name = "scraped_at", nullable = false, updatable = false)
    private LocalDateTime scrapedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
