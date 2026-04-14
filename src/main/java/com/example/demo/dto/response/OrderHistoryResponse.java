package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderHistoryResponse {
    Integer orderId;
    BigDecimal totalAmount;
    String orderStatus;
    String paymentStatus;
    LocalDateTime createdAt;

    // Danh sách mặt hàng để FE render ảnh và tên sản phẩm
    List<OrderItemPreviewResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderItemPreviewResponse {
        Integer skuId;
        String productName;
        String imageUrl;
        Integer quantity;
        BigDecimal price;
    }
}