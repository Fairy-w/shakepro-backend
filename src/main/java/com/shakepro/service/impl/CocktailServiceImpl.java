package com.shakepro.service.impl;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.dto.response.BannerResponse;
import com.shakepro.dto.response.CategoryResponse;
import com.shakepro.dto.response.CocktailDetailResponse;
import com.shakepro.dto.response.CocktailListResponse;
import com.shakepro.entity.Cocktail;
import com.shakepro.entity.CocktailMaterial;
import com.shakepro.repository.CocktailMaterialRepository;
import com.shakepro.repository.CocktailRepository;
import com.shakepro.service.CocktailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CocktailServiceImpl implements CocktailService {

    private final CocktailRepository cocktailRepository;
    private final CocktailMaterialRepository cocktailMaterialRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_BANNER = "cache:cocktail:banner";

    @Override
    public Page<CocktailListResponse> listCocktails(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Cocktail> cocktailPage;
        if (keyword != null && !keyword.isBlank()) {
            cocktailPage = cocktailRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            cocktailPage = cocktailRepository.findAll(pageable);
        }

        return cocktailPage.map(CocktailListResponse::from);
    }

    @Override
    public CocktailDetailResponse getCocktailDetail(Long id) {
        Cocktail cocktail = cocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));

        List<CocktailMaterial> materials = cocktailMaterialRepository.findByCocktailId(id);

        List<CocktailDetailResponse.MaterialItemResponse> materialItems = materials.stream()
                .map(cm -> CocktailDetailResponse.MaterialItemResponse.builder()
                        .materialId(cm.getMaterial().getId())
                        .name(cm.getMaterial().getName())
                        .category(cm.getMaterial().getCategory())
                        .amount(cm.getAmount())
                        .build())
                .toList();

        return CocktailDetailResponse.builder()
                .id(cocktail.getId())
                .name(cocktail.getName())
                .description(cocktail.getDescription())
                .imageUrl(cocktail.getImageUrl())
                .alcoholLevel(cocktail.getAlcoholLevel())
                .steps(cocktail.getSteps())
                .materials(materialItems)
                .createdAt(cocktail.getCreatedAt())
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BannerResponse> getBanners() {
        // Try cache first
        Object cached = redisTemplate.opsForValue().get(CACHE_BANNER);
        if (cached instanceof List<?> list) {
            return (List<BannerResponse>) list;
        }

        Pageable top5 = PageRequest.of(0, 5);
        List<Cocktail> cocktails = cocktailRepository.findTopBanner(top5);

        List<BannerResponse> banners = cocktails.stream()
                .map(c -> BannerResponse.builder()
                        .cocktailId(c.getId())
                        .imageUrl(c.getImageUrl())
                        .title(c.getName())
                        .build())
                .toList();

        redisTemplate.opsForValue().set(CACHE_BANNER, banners, 300, TimeUnit.SECONDS);
        return banners;
    }

    @Override
    public List<CategoryResponse> getCategories() {
        // Return fixed categories for cocktail types
        return Arrays.asList(
                CategoryResponse.builder().name("经典鸡尾酒").icon("classic").build(),
                CategoryResponse.builder().name("烈酒基底").icon("spirit").build(),
                CategoryResponse.builder().name("果味清爽").icon("fruity").build(),
                CategoryResponse.builder().name("低酒精").icon("low_alcohol").build(),
                CategoryResponse.builder().name("无酒精").icon("mocktail").build(),
                CategoryResponse.builder().name("热饮").icon("hot").build(),
                CategoryResponse.builder().name("派对特饮").icon("party").build(),
                CategoryResponse.builder().name("创意特调").icon("creative").build()
        );
    }
}
