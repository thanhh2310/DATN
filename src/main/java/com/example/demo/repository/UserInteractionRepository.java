package com.example.demo.repository;

import com.example.demo.model.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {
    
    List<UserInteraction> findByUserIdOrderByCreatedAtDesc(Integer userId);
    
    List<UserInteraction> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    @Query("""
    SELECT ui FROM UserInteraction ui
    WHERE ui.user.id = :userId
    AND ui.product.id = :productId
    ORDER BY ui.createdAt DESC
""")
    List<UserInteraction> findLatestByUserIdAndProductId(
            @Param("userId") Integer userId,
            @Param("productId") Integer productId
    );
    
    @Query("SELECT ui FROM UserInteraction ui WHERE ui.sessionId = :sessionId AND ui.product.id = :productId ORDER BY ui.createdAt DESC LIMIT 1")
    UserInteraction findLatestBySessionIdAndProductId(@Param("sessionId") String sessionId, @Param("productId") Integer productId);

    @Query("""
    SELECT ui.product.id, SUM(ui.interactionWeight)
    FROM UserInteraction ui
    WHERE ui.user.id = :userId
      AND ui.product IS NOT NULL
    GROUP BY ui.product.id
    ORDER BY SUM(ui.interactionWeight) DESC
""")
    List<Object[]> getProductScoresByUserId(@Param("userId") Integer userId);
}
