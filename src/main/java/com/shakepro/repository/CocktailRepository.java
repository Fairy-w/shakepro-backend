package com.shakepro.repository;

import com.shakepro.entity.Cocktail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CocktailRepository extends JpaRepository<Cocktail, Long> {

    Page<Cocktail> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT c FROM Cocktail c ORDER BY c.createdAt DESC")
    List<Cocktail> findTopBanner(Pageable pageable);

    @Query("SELECT DISTINCT cm.cocktail FROM CocktailMaterial cm WHERE cm.material.name IN :materials")
    List<Cocktail> findByMaterialNames(@Param("materials") List<String> materials);
}
