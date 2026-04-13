package com.shakepro.repository;

import com.shakepro.entity.RecipeSourceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeSourceRecordRepository extends JpaRepository<RecipeSourceRecord, Long> {

    Optional<RecipeSourceRecord> findBySourceUrl(String sourceUrl);

    List<RecipeSourceRecord> findByStatusOrderByScrapedAtDesc(String status);

    List<RecipeSourceRecord> findBySourceSiteOrderByScrapedAtDesc(String sourceSite);
}
