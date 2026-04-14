package com.example.demo.repository;

import com.example.demo.model.Product;
import com.example.demo.model.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Integer> {
    boolean existsBySkuCodeIn(Collection<String> skuCodes);

    @Modifying
    @Query("UPDATE ProductSku s SET s.stockQuantity = s.stockQuantity - :qty WHERE s.id = :skuId AND s.stockQuantity >= :qty")
    int decrementStock(@Param("skuId") Integer skuId, @Param("qty") Integer qty);

    @Modifying
    @Query("UPDATE ProductSku s SET s.stockQuantity = s.stockQuantity + :qty WHERE s.id = :skuId")
    void incrementStock(@Param("skuId") Integer skuId, @Param("qty") Integer qty);
}
