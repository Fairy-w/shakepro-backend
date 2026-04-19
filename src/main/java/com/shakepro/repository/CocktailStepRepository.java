package com.shakepro.repository;

import com.shakepro.entity.CocktailStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CocktailStepRepository extends JpaRepository<CocktailStep, Long> {

    List<CocktailStep> findByCocktailIdOrderByStepOrderAsc(Long cocktailId);

    void deleteByCocktailId(Long cocktailId);
}
