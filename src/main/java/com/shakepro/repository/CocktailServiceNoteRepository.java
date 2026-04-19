package com.shakepro.repository;

import com.shakepro.entity.CocktailServiceNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CocktailServiceNoteRepository extends JpaRepository<CocktailServiceNote, Long> {

    List<CocktailServiceNote> findByCocktailIdOrderBySortOrderAsc(Long cocktailId);

    void deleteByCocktailId(Long cocktailId);
}
