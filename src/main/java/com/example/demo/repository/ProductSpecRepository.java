package com.example.demo.repository;

import com.example.demo.model.Product;
import com.example.demo.model.ProductSpec;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, Integer> {
}
