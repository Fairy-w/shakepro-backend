package com.shakepro.service;

import com.shakepro.dto.response.MaterialResponse;

import java.util.List;

public interface MaterialService {

    List<MaterialResponse> listMaterials(String keyword);

    List<String> listCategories();
}
