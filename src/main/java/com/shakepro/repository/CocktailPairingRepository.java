package com.shakepro.repository;

import com.shakepro.entity.CocktailPairing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CocktailPairingRepository extends JpaRepository<CocktailPairing, Long> {

    List<CocktailPairing> findByCocktailIdOrderBySortOrderAsc(Long cocktailId);

    void deleteByCocktailId(Long cocktailId);
}
