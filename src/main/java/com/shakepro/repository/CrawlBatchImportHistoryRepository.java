package com.shakepro.repository;

import com.shakepro.entity.CrawlBatchImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlBatchImportHistoryRepository extends JpaRepository<CrawlBatchImportHistory, Long> {
}
