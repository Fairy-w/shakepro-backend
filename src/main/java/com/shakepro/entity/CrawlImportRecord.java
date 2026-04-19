package com.shakepro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_import_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlImportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "detail_url", nullable = false, length = 768, unique = true)
    private String detailUrl;

    @Column(name = "list_url", length = 1000)
    private String listUrl;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "saved_cocktail_id")
    private Long savedCocktailId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "last_crawled_at", nullable = false)
    private LocalDateTime lastCrawledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
