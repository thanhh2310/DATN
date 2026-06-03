package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemResponse {
    Integer id;
    Integer skuId;
    Integer stockQuantity;
    Integer productId;
    String productName;
    String imageUrl;
    Integer quantity;
    java.math.BigDecimal price;

    List<SkuAttributeResponse> attributeValues;
}
