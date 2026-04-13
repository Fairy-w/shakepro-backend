package com.shakepro.repository;

import com.shakepro.entity.RecipeDetailContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeDetailContentRepository extends JpaRepository<RecipeDetailContent, Long> {

    Optional<RecipeDetailContent> findByRecipeKey(String recipeKey);

    Optional<RecipeDetailContent> findByStructuredRecordId(Long structuredRecordId);

    List<RecipeDetailContent> findByStatusOrderByUpdatedAtDesc(String status);
}
