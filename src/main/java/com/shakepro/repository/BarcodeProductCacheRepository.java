package com.shakepro.repository;

import com.shakepro.entity.BarcodeProductCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BarcodeProductCacheRepository extends JpaRepository<BarcodeProductCache, Long> {

    Optional<BarcodeProductCache> findByBarcode(String barcode);
}
