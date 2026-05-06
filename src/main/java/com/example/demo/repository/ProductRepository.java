package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
    // Dùng EntityGraph để kéo luôn category và brand lên trong 1 nốt nhạc (Tránh N+1)
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
            // Mẹo: Nếu keyword rỗng, bỏ qua vụ LIKE để query chạy siêu tốc
            "AND (:keyword = '' OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
