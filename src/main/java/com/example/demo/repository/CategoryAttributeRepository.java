package com.example.demo.repository;

import com.example.demo.model.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, Integer> {
    List<CategoryAttribute> findByCategoryId(Integer categoryId);
    boolean existsByCategoryIdAndAttributeId(Integer categoryId, Integer attributeId);
}
