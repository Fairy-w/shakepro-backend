package com.shakepro.service.impl;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.dto.response.CocktailListResponse;
import com.shakepro.entity.Cocktail;
import com.shakepro.entity.Favorite;
import com.shakepro.entity.User;
import com.shakepro.repository.CocktailRepository;
import com.shakepro.repository.FavoriteRepository;
import com.shakepro.repository.UserRepository;
import com.shakepro.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final CocktailRepository cocktailRepository;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long cocktailId) {
        if (favoriteRepository.existsByUserIdAndCocktailId(userId, cocktailId)) {
            return; // Already favorited, idempotent
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        Cocktail cocktail = cocktailRepository.findById(cocktailId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));

        Favorite favorite = Favorite.builder()
                .user(user)
                .cocktail(cocktail)
                .build();
        favoriteRepository.save(favorite);
        log.info("User {} favorited cocktail {}", userId, cocktailId);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long cocktailId) {
        Favorite favorite = favoriteRepository.findByUserIdAndCocktailId(userId, cocktailId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "收藏记录不存在"));
        favoriteRepository.delete(favorite);
        log.info("User {} unfavorited cocktail {}", userId, cocktailId);
    }

    @Override
    public List<CocktailListResponse> listFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(f -> CocktailListResponse.from(f.getCocktail()))
                .toList();
    }
}
