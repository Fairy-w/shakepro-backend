package com.shakepro.repository;

import com.shakepro.entity.CocktailMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CocktailMaterialRepository extends JpaRepository<CocktailMaterial, Long> {

    List<CocktailMaterial> findByCocktailId(Long cocktailId);

    boolean existsByMaterialId(Long materialId);

    void deleteByCocktailId(Long cocktailId);
}
