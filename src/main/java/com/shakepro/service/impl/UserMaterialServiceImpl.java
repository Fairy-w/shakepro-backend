package com.shakepro.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.OssImageUrlBuilder;
import com.shakepro.common.util.ScanFieldUtils;
import com.shakepro.dto.request.UserMaterialManualSaveRequest;
import com.shakepro.dto.request.UserMaterialSaveRequest;
import com.shakepro.dto.response.UserMaterialResponse;
import com.shakepro.entity.Material;
import com.shakepro.entity.User;
import com.shakepro.entity.UserMaterial;
import com.shakepro.repository.MaterialRepository;
import com.shakepro.repository.UserMaterialRepository;
import com.shakepro.repository.UserRepository;
import com.shakepro.service.UserMaterialService;
import com.shakepro.service.support.MaterialAliasMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMaterialServiceImpl implements UserMaterialService {

    private final UserMaterialRepository userMaterialRepository;
    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;
    private final MaterialAliasMatcher materialAliasMatcher;
    private final OssImageUrlBuilder ossImageUrlBuilder;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UserMaterialResponse saveFromScan(Long userId, UserMaterialSaveRequest request) {
        String barcode = ScanFieldUtils.normalizeBarcode(request.getBarcode());
        if (barcode.length() < 8 || barcode.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "barcode长度必须在8到32之间");
        }

        User user = requireUser(userId);

        UserMaterial materialRecord = userMaterialRepository.findByUserIdAndBarcode(userId, barcode)
                .orElse(UserMaterial.builder()
                        .user(user)
                        .barcode(barcode)
                        .build());

        List<String> tags = sanitizeTags(request.getTags());
        MaterialAliasMatcher.MatchResult match = materialAliasMatcher.match(request.getName(), request.getBrand(), tags);
        Material material = resolveMaterial(request.getMaterialId(), match.materialId(), materialRecord.getMaterial());

        materialRecord.setMaterial(material);
        materialRecord.setSource(firstNonBlank(request.getSource(), materialRecord.getSource(), "scan"));
        materialRecord.setName(request.getName().trim());
        materialRecord.setBrand(ScanFieldUtils.trimToNull(request.getBrand()));
        materialRecord.setCategoryId(firstNonBlank(request.getCategoryId(), match.categoryId(), material != null ? material.getCategory() : null));
        materialRecord.setBarcode(barcode);
        // Temporarily disable capacity/inventory tracking and only keep presence marker.
        materialRecord.setCapacityText(null);
        materialRecord.setRemainLevel("full");
        materialRecord.setOpened(false);
        materialRecord.setHasItem(request.getHasItem() != null ? request.getHasItem() : (materialRecord.getHasItem() == null || materialRecord.getHasItem()));
        materialRecord.setTagsJson(writeTags(tags));

        UserMaterial saved = userMaterialRepository.save(materialRecord);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserMaterialResponse saveManual(Long userId, UserMaterialManualSaveRequest request) {
        User user = requireUser(userId);
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "材料不存在"));

        UserMaterial materialRecord = userMaterialRepository
                .findFirstByUserIdAndMaterialIdOrderByUpdatedAtDesc(userId, request.getMaterialId())
                .orElseGet(() -> UserMaterial.builder()
                        .user(user)
                        .barcode(manualBarcode(userId, request.getMaterialId()))
                        .build());

        List<String> tags = sanitizeTags(request.getTags());
        materialRecord.setMaterial(material);
        materialRecord.setSource(firstNonBlank(request.getSource(), materialRecord.getSource(), "manual"));
        materialRecord.setName(material.getName());
        materialRecord.setBrand(ScanFieldUtils.trimToNull(request.getBrand()));
        materialRecord.setCategoryId(firstNonBlank(request.getCategoryId(), materialRecord.getCategoryId(), material.getCategory()));
        // Temporarily disable capacity/inventory tracking and only keep presence marker.
        materialRecord.setCapacityText(null);
        materialRecord.setRemainLevel("full");
        materialRecord.setOpened(false);
        materialRecord.setHasItem(request.getHasItem() != null ? request.getHasItem() : (materialRecord.getHasItem() == null || materialRecord.getHasItem()));
        materialRecord.setTagsJson(writeTags(tags));

        UserMaterial saved = userMaterialRepository.save(materialRecord);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<UserMaterialResponse> saveManualBatch(Long userId, List<UserMaterialManualSaveRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "items不能为空");
        }
        return requests.stream()
                .map(request -> saveManual(userId, request))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserMaterialResponse> list(Long userId, String keyword, String categoryId) {
        String normalizedKeyword = ScanFieldUtils.trimToNull(keyword);
        String normalizedCategoryId = ScanFieldUtils.trimToNull(categoryId);

        List<UserMaterial> records;
        if (normalizedKeyword != null && normalizedCategoryId != null) {
            records = userMaterialRepository.findByUserIdAndCategoryIdAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(
                    userId,
                    normalizedCategoryId,
                    normalizedKeyword
            );
        } else if (normalizedKeyword != null) {
            records = userMaterialRepository.findByUserIdAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(userId, normalizedKeyword);
        } else if (normalizedCategoryId != null) {
            records = userMaterialRepository.findByUserIdAndCategoryIdOrderByUpdatedAtDesc(userId, normalizedCategoryId);
        } else {
            records = userMaterialRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }

        return records.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void removeByBarcode(Long userId, String barcode) {
        String normalizedBarcode = ScanFieldUtils.normalizeBarcode(barcode);
        UserMaterial materialRecord = userMaterialRepository.findByUserIdAndBarcode(userId, normalizedBarcode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "该条码对应的用户材料不存在"));
        userMaterialRepository.delete(materialRecord);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    private Material resolveMaterial(Long requestMaterialId, Long matchedMaterialId, Material existingMaterial) {
        if (requestMaterialId != null) {
            return materialRepository.findById(requestMaterialId).orElse(existingMaterial);
        }
        if (matchedMaterialId != null) {
            return materialRepository.findById(matchedMaterialId).orElse(existingMaterial);
        }
        return existingMaterial;
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (Exception e) {
            log.warn("Serialize tags failed: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> readTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(tagsJson);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> tags = new ArrayList<>();
            for (JsonNode item : node) {
                String text = ScanFieldUtils.trimToNull(item.asText());
                if (text != null) {
                    tags.add(text);
                }
            }
            return tags;
        } catch (Exception e) {
            log.warn("Parse tags json failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> sanitizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(ScanFieldUtils::trimToNull)
                .filter(value -> value != null && value.length() <= 32)
                .distinct()
                .limit(6)
                .toList();
    }

    private UserMaterialResponse toResponse(UserMaterial materialRecord) {
        String imageUrl = materialRecord.getMaterial() != null ? materialRecord.getMaterial().getImageUrl() : null;
        return UserMaterialResponse.builder()
                .id(materialRecord.getId())
                .userId(materialRecord.getUser() != null ? materialRecord.getUser().getId() : null)
                .barcode(materialRecord.getBarcode())
                .materialId(materialRecord.getMaterial() != null ? materialRecord.getMaterial().getId() : null)
                .source(materialRecord.getSource())
                .imageUrl(imageUrl)
                .imageUrlThumb(ossImageUrlBuilder.toThumbUrl(imageUrl))
                .imageUrlCard(ossImageUrlBuilder.toCardUrl(imageUrl))
                .imageUrlDetail(ossImageUrlBuilder.toDetailUrl(imageUrl))
                .name(materialRecord.getName())
                .brand(materialRecord.getBrand())
                .categoryId(materialRecord.getCategoryId())
                .capacityText(null)
                .remainLevel(null)
                .opened(null)
                .hasItem(Boolean.TRUE.equals(materialRecord.getHasItem()))
                .tags(readTags(materialRecord.getTagsJson()))
                .createdAt(materialRecord.getCreatedAt())
                .updatedAt(materialRecord.getUpdatedAt())
                .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = ScanFieldUtils.trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String manualBarcode(Long userId, Long materialId) {
        return "99" + fixedTailDigits(userId, 10) + fixedTailDigits(materialId, 10);
    }

    private String fixedTailDigits(Long value, int width) {
        String digits = String.valueOf(Math.abs(value == null ? 0L : value));
        if (digits.length() > width) {
            digits = digits.substring(digits.length() - width);
        }
        return "0".repeat(width - digits.length()) + digits;
    }
}
