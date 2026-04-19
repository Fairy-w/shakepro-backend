package com.shakepro.service.impl;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.OssImageUrlBuilder;
import com.shakepro.dto.response.BannerResponse;
import com.shakepro.dto.response.CategoryResponse;
import com.shakepro.dto.response.CocktailDetailResponse;
import com.shakepro.dto.response.CocktailListResponse;
import com.shakepro.entity.Cocktail;
import com.shakepro.entity.CocktailFlavorMetric;
import com.shakepro.entity.CocktailFlavorTag;
import com.shakepro.entity.CocktailMaterial;
import com.shakepro.entity.CocktailPairing;
import com.shakepro.entity.CocktailServiceNote;
import com.shakepro.entity.CocktailStep;
import com.shakepro.repository.CocktailFlavorMetricRepository;
import com.shakepro.repository.CocktailFlavorTagRepository;
import com.shakepro.repository.CocktailMaterialRepository;
import com.shakepro.repository.CocktailPairingRepository;
import com.shakepro.repository.CocktailRepository;
import com.shakepro.repository.CocktailServiceNoteRepository;
import com.shakepro.repository.CocktailStepRepository;
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
    private final CocktailStepRepository cocktailStepRepository;
    private final CocktailFlavorTagRepository cocktailFlavorTagRepository;
    private final CocktailFlavorMetricRepository cocktailFlavorMetricRepository;
    private final CocktailPairingRepository cocktailPairingRepository;
    private final CocktailServiceNoteRepository cocktailServiceNoteRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OssImageUrlBuilder ossImageUrlBuilder;

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

        return cocktailPage.map(this::buildCocktailListResponse);
    }

    @Override
    public CocktailDetailResponse getCocktailDetail(Long id) {
        Cocktail cocktail = cocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));
        return buildCocktailDetailResponse(cocktail);
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
                        .imageUrl(c.getHeroImage() != null ? c.getHeroImage() : c.getImageUrl())
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
                CategoryResponse.builder().name("全部").icon("all").build(),
                CategoryResponse.builder().name("入门").icon("beginner").build(),
                CategoryResponse.builder().name("金酒").icon("gin").build(),
                CategoryResponse.builder().name("伏特加").icon("vodka").build(),
                CategoryResponse.builder().name("朗姆").icon("rum").build(),
                CategoryResponse.builder().name("龙舌兰").icon("tequila").build(),
                CategoryResponse.builder().name("威士忌").icon("whiskey").build(),
                CategoryResponse.builder().name("白兰地").icon("brandy").build(),
                CategoryResponse.builder().name("利口酒").icon("liqueur").build(),
                CategoryResponse.builder().name("无醇").icon("non_alcoholic").build()
        );
    }

    private CocktailDetailResponse buildCocktailDetailResponse(Cocktail cocktail) {
        Long cocktailId = cocktail.getId();
        String preferredImageUrl = cocktail.getHeroImage() != null ? cocktail.getHeroImage() : cocktail.getImageUrl();
        List<CocktailMaterial> materials = cocktailMaterialRepository.findByCocktailIdOrderBySortOrderAscIdAsc(cocktailId);
        List<CocktailStep> steps = cocktailStepRepository.findByCocktailIdOrderByStepOrderAsc(cocktailId);
        List<CocktailFlavorTag> flavorTags = cocktailFlavorTagRepository.findByCocktailIdOrderBySortOrderAsc(cocktailId);
        List<CocktailFlavorMetric> flavorMetrics = cocktailFlavorMetricRepository.findByCocktailIdOrderBySortOrderAsc(cocktailId);
        List<CocktailPairing> pairings = cocktailPairingRepository.findByCocktailIdOrderBySortOrderAsc(cocktailId);
        List<CocktailServiceNote> serviceNotes = cocktailServiceNoteRepository.findByCocktailIdOrderBySortOrderAsc(cocktailId);

        return CocktailDetailResponse.builder()
                .id(cocktailId)
                .name(cocktail.getName())
                .englishName(cocktail.getEnglishName())
                .category(cocktail.getCategory())
                .heroImage(cocktail.getHeroImage())
                .difficulty(cocktail.getDifficulty())
                .abv(cocktail.getAbv())
                .glass(cocktail.getGlass())
                .garnish(cocktail.getGarnish())
                .highlight(cocktail.getHighlight())
                .subtitle(cocktail.getSubtitle())
                .description(cocktail.getDescription())
                .story(cocktail.getStory())
                .imageUrl(preferredImageUrl)
                .heroImageCard(ossImageUrlBuilder.toCardUrl(preferredImageUrl))
                .heroImageDetail(ossImageUrlBuilder.toDetailUrl(preferredImageUrl))
                .alcoholLevel(cocktail.getAlcoholLevel())
                .legacySteps(cocktail.getSteps())
                .flavorTags(flavorTags.stream().map(CocktailFlavorTag::getTag).toList())
                .flavorMetrics(flavorMetrics.stream()
                        .map(metric -> CocktailDetailResponse.FlavorMetricItemResponse.builder()
                                .sortOrder(metric.getSortOrder())
                                .name(metric.getMetricName())
                                .value(metric.getMetricValue())
                                .build())
                        .toList())
                .pairings(pairings.stream().map(CocktailPairing::getPairing).toList())
                .serviceNotes(serviceNotes.stream().map(CocktailServiceNote::getNote).toList())
                .steps(steps.stream()
                        .map(step -> CocktailDetailResponse.StepItemResponse.builder()
                                .order(step.getStepOrder())
                                .title(step.getTitle())
                                .detail(step.getDetail())
                                .build())
                        .toList())
                .materials(materials.stream()
                        .map(cm -> CocktailDetailResponse.MaterialItemResponse.builder()
                                .materialId(cm.getMaterial() != null ? cm.getMaterial().getId() : null)
                                .name(cm.getMaterial() != null ? cm.getMaterial().getName() : cm.getDisplayName())
                                .category(cm.getMaterial() != null ? cm.getMaterial().getCategory() : null)
                                .displayName(cm.getDisplayName())
                                .amount(cm.getAmount())
                                .note(cm.getNote())
                                .sortOrder(cm.getSortOrder())
                                .build())
                        .toList())
                .createdAt(cocktail.getCreatedAt())
                .build();
    }

    private CocktailListResponse buildCocktailListResponse(Cocktail cocktail) {
        String preferredImageUrl = cocktail.getHeroImage() != null ? cocktail.getHeroImage() : cocktail.getImageUrl();
        return CocktailListResponse.builder()
                .id(cocktail.getId())
                .name(cocktail.getName())
                .englishName(cocktail.getEnglishName())
                .category(cocktail.getCategory())
                .heroImage(cocktail.getHeroImage())
                .difficulty(cocktail.getDifficulty())
                .abv(cocktail.getAbv())
                .imageUrl(preferredImageUrl)
                .imageUrlThumb(ossImageUrlBuilder.toThumbUrl(preferredImageUrl))
                .imageUrlCard(ossImageUrlBuilder.toCardUrl(preferredImageUrl))
                .description(cocktail.getDescription())
                .alcoholLevel(cocktail.getAlcoholLevel())
                .build();
    }
}
