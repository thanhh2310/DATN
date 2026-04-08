package com.example.demo.repository;

import com.example.demo.model.Category;
import com.example.demo.model.Product;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsBySlug(@NotBlank(message = "Slug không được để trống") String slug);
    List<Category> findByParentIsNull();
}
