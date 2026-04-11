package com.shakepro.repository;

import com.shakepro.entity.FavoriteAiCocktail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FavoriteAiCocktailRepository extends JpaRepository<FavoriteAiCocktail, Long> {

    Optional<FavoriteAiCocktail> findByUserIdAndRecipeKey(Long userId, String recipeKey);

    Optional<FavoriteAiCocktail> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT favorite
            FROM FavoriteAiCocktail favorite
            WHERE favorite.userId = :userId
              AND (
                    :keyword IS NULL
                    OR LOWER(favorite.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(favorite.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(favorite.prompt, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<FavoriteAiCocktail> search(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    @Query(
            value = """
                    SELECT favorite
                    FROM FavoriteAiCocktail favorite, User user
                    WHERE favorite.userId = user.id
                      AND (
                            :keyword IS NULL
                            OR LOWER(favorite.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(favorite.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(favorite.prompt, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(favorite.recipeKey) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(user.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(user.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(favorite)
                    FROM FavoriteAiCocktail favorite, User user
                    WHERE favorite.userId = user.id
                      AND (
                            :keyword IS NULL
                            OR LOWER(favorite.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(favorite.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(favorite.prompt, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(favorite.recipeKey) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(user.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(user.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """
    )
    Page<FavoriteAiCocktail> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
