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

import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_batch_import_histories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlBatchImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_url", nullable = false, length = 1000)
    private String listUrl;

    @Column(name = "list_title", length = 255)
    private String listTitle;

    @Column(name = "only_new", nullable = false)
    private Boolean onlyNew;

    @Column(name = "max_items", nullable = false)
    private Integer maxItems;

    @Column(nullable = false)
    private Integer concurrency;

    @Column(name = "auto_generate", nullable = false)
    private Boolean autoGenerate;

    @Column(name = "auto_save", nullable = false)
    private Boolean autoSave;

    @Column(name = "discovered_count", nullable = false)
    private Integer discoveredCount;

    @Column(name = "selected_count", nullable = false)
    private Integer selectedCount;

    @Column(name = "processed_count", nullable = false)
    private Integer processedCount;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount;

    @Column(name = "remaining_unimported_count", nullable = false)
    private Integer remainingUnimportedCount;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
