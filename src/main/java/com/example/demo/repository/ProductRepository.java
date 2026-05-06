package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer>,
        JpaSpecificationExecutor<Product> {

    boolean existsBySlug(String slug);
    boolean existsByName(String name);

    boolean existsBySlugAndIdNot(String slug, Integer id);
    boolean existsByNameAndIdNot(String name, Integer id);

    // Search API: Tìm theo tên hoặc mô tả, chỉ lấy sản phẩm active
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
