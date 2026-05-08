package com.example.demo.repository;

import com.example.demo.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    
    Page<Review> findByProductIdAndIsApprovedTrue(Integer productId, Pageable pageable);
    
    Page<Review> findByUserId(Integer userId, Pageable pageable);
    
    boolean existsByUserIdAndProductIdAndOrderItemId(Integer userId, Integer productId, Integer orderItemId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Double getAverageRatingByProductId(@Param("productId") Integer productId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Long countByProductIdAndIsApprovedTrue(@Param("productId") Integer productId);
    
    Optional<Review> findByUserIdAndOrderItemId(Integer userId, Integer orderItemId);
}
