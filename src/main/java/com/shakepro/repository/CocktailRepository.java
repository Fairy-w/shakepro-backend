package com.shakepro.repository;

import com.shakepro.entity.Cocktail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface CocktailRepository extends JpaRepository<Cocktail, Long> {

    Page<Cocktail> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Cocktail> findByCategoryIgnoreCase(String category, Pageable pageable);

    Page<Cocktail> findByNameContainingIgnoreCaseAndCategoryIgnoreCase(String keyword, String category, Pageable pageable);

    @Query("SELECT c FROM Cocktail c ORDER BY c.createdAt DESC")
    List<Cocktail> findTopBanner(Pageable pageable);

    @Query("SELECT DISTINCT cm.cocktail FROM CocktailMaterial cm WHERE cm.material.name IN :materials")
    List<Cocktail> findByMaterialNames(@Param("materials") List<String> materials);

    Optional<Cocktail> findBySourceUrl(String sourceUrl);

    @Query("SELECT c.sourceUrl FROM Cocktail c WHERE c.sourceUrl IN :sourceUrls")
    List<String> findExistingSourceUrlsIn(@Param("sourceUrls") Collection<String> sourceUrls);

    @Query("SELECT DISTINCT c.category FROM Cocktail c WHERE c.category IS NOT NULL AND TRIM(c.category) <> '' ORDER BY c.category")
    List<String> findDistinctCategories();
}
