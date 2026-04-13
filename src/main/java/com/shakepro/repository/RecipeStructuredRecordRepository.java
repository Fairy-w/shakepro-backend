package com.shakepro.repository;

import com.shakepro.entity.RecipeStructuredRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeStructuredRecordRepository extends JpaRepository<RecipeStructuredRecord, Long> {

    Optional<RecipeStructuredRecord> findByRecipeKey(String recipeKey);

    Optional<RecipeStructuredRecord> findBySourceRecordId(Long sourceRecordId);

    List<RecipeStructuredRecord> findByStatusOrderByParsedAtDesc(String status);
}
