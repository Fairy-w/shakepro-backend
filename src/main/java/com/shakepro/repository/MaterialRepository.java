package com.shakepro.repository;

import com.shakepro.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByNameIgnoreCase(String name);

    List<Material> findByNameContainingIgnoreCase(String keyword);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT DISTINCT m.category FROM Material m WHERE m.category IS NOT NULL ORDER BY m.category")
    List<String> findDistinctCategories();

    List<Material> findByCategory(String category);
}
