package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(nullable = false, unique = true, length = 50)
    String code;

    @Column(name = "discount_type", nullable = false, length = 50)
    String discountType; // Cân nhắc dùng Enum nếu muốn chặt chẽ

    @Column(name = "discount_value", nullable = false)
    BigDecimal discountValue;

    @Builder.Default
    @Column(name = "min_order_value")
    BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "usage_limit")
    Integer usageLimit;

    @Builder.Default
    @Column(name = "used_count")
    Integer usedCount = 0;

    @Column(name = "start_date")
    LocalDateTime startDate;

    @Column(name = "end_date")
    LocalDateTime endDate;

    @Builder.Default
    @Column(name = "is_active")
    Boolean isActive = true;
}