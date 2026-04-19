package com.shakepro.repository;

import com.shakepro.entity.CocktailFlavorTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CocktailFlavorTagRepository extends JpaRepository<CocktailFlavorTag, Long> {

    List<CocktailFlavorTag> findByCocktailIdOrderBySortOrderAsc(Long cocktailId);

    void deleteByCocktailId(Long cocktailId);
}
