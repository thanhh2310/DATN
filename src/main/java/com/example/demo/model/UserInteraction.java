package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_interactions")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id; // Dùng Long map với BIGSERIAL của PostgreSQL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Nullable cho khách chưa đăng nhập
    User user;

    @Column(name = "session_id", nullable = false, length = 100)
    String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id") // Nullable vì có thể khách chỉ tìm kiếm (SEARCH)
    Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    InteractionType interactionType;

    @Builder.Default
    @Column(name = "interaction_weight", nullable = false)
    Float interactionWeight = 1.0f; // Trọng số: Mua (5.0), Giỏ hàng (3.0), Xem (1.0)

    @Column(name = "search_keyword")
    String searchKeyword;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    public enum InteractionType {
        VIEW, SEARCH, ADD_TO_CART, PURCHASE
    }
}