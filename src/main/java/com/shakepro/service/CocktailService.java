package com.shakepro.service;

import com.shakepro.dto.response.BannerResponse;
import com.shakepro.dto.response.CategoryResponse;
import com.shakepro.dto.response.CocktailDetailResponse;
import com.shakepro.dto.response.CocktailListResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CocktailService {

    Page<CocktailListResponse> listCocktails(String keyword, int page, int size);

    CocktailDetailResponse getCocktailDetail(Long id);

    List<BannerResponse> getBanners();

    List<CategoryResponse> getCategories();
}
