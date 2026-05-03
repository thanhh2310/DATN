package com.example.demo.repository;

import com.example.demo.model.ProductSkuImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSkuImageRepository extends JpaRepository<ProductSkuImage, Integer> {
    List<ProductSkuImage> findByProductSkuIdOrderByDisplayOrderAsc(Integer productSkuId);
    
    Optional<ProductSkuImage> findByProductSkuIdAndIsThumbnailTrue(Integer productSkuId);
}
