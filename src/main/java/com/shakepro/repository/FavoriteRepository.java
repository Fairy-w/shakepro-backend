package com.shakepro.repository;

import com.shakepro.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    Optional<Favorite> findByUserIdAndCocktailId(Long userId, Long cocktailId);

    boolean existsByUserIdAndCocktailId(Long userId, Long cocktailId);
}
