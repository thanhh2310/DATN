package com.example.demo.repository;

import com.example.demo.model.Product;
import com.example.demo.model.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Integer> {
    boolean existsBySkuCodeIn(Collection<String> skuCodes);
}
