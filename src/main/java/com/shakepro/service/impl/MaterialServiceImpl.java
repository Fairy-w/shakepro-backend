package com.shakepro.service.impl;

import com.shakepro.dto.response.MaterialResponse;
import com.shakepro.entity.Material;
import com.shakepro.repository.MaterialRepository;
import com.shakepro.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;

    @Override
    public List<MaterialResponse> listMaterials(String keyword) {
        List<Material> materials;
        if (keyword != null && !keyword.isBlank()) {
            materials = materialRepository.findByNameContainingIgnoreCase(keyword.trim());
        } else {
            materials = materialRepository.findAll();
        }
        return materials.stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @Override
    public List<String> listCategories() {
        return materialRepository.findDistinctCategories();
    }
}
