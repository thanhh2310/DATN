package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponResponse {

    Integer id;
    String code;
    String discountType;
    BigDecimal discountValue;
    BigDecimal minOrderValue;
    Integer usageLimit;
    Integer usedCount;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Boolean isActive;

}