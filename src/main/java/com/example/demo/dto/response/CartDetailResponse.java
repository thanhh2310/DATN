package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartDetailResponse {
    Integer id;
    Integer userId;
    String sessionId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    List<CartItemResponse> items;
    java.math.BigDecimal totalAmount;
}