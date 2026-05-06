package com.example.demo.repository;

import com.example.demo.model.ProductSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, Integer> {
    
    Optional<ProductSpec> findByProductIdAndAttributeValueId(Integer productId, Integer attributeValueId);

    boolean existsByProductIdAndAttributeValueId(Integer productId, Integer attributeValueId);

    @Modifying
    @Query("DELETE FROM ProductSpec ps WHERE ps.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);
}
