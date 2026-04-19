package com.shakepro.repository;

import com.shakepro.entity.UserMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMaterialRepository extends JpaRepository<UserMaterial, Long> {

    Optional<UserMaterial> findByUserIdAndBarcode(Long userId, String barcode);

    Optional<UserMaterial> findFirstByUserIdAndMaterialIdOrderByUpdatedAtDesc(Long userId, Long materialId);

    List<UserMaterial> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<UserMaterial> findByUserIdAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(Long userId, String keyword);

    List<UserMaterial> findByUserIdAndCategoryIdOrderByUpdatedAtDesc(Long userId, String categoryId);

    List<UserMaterial> findByUserIdAndCategoryIdAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(
            Long userId,
            String categoryId,
            String keyword
    );
}
