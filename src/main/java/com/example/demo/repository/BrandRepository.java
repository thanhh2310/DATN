package com.example.demo.repository;

import com.example.demo.model.Brand;
import com.example.demo.model.Product;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Integer> {
    boolean existsBySlug(@NotBlank(message = "Slug không được để trống") String slug);
    List<Brand> findByIsActiveTrue(Sort sort);
}
