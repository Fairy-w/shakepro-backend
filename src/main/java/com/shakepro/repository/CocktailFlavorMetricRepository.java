package com.shakepro.repository;

import com.shakepro.entity.CocktailFlavorMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CocktailFlavorMetricRepository extends JpaRepository<CocktailFlavorMetric, Long> {

    List<CocktailFlavorMetric> findByCocktailIdOrderBySortOrderAsc(Long cocktailId);

    void deleteByCocktailId(Long cocktailId);
}
