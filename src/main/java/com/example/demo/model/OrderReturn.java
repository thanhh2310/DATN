package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_returns",
        indexes = {
                @Index(name = "idx_order_returns_order_id", columnList = "order_id"),
                @Index(name = "idx_order_returns_user_id", columnList = "user_id"),
                @Index(name = "idx_order_returns_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    String reason;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    String adminNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ReturnStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    User processedBy;

    @Column(name = "processed_at")
    LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public enum ReturnStatus {
        PENDING, APPROVED, REJECTED
    }
}
