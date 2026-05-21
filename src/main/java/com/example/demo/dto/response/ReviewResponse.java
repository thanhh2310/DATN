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
public class ReviewResponse {
    Integer id;
    Integer userId;
    String userName;
    Integer productId;
    String productName;
    String productSlug;
    String productImage;
    Integer orderItemId;
    Integer skuId;
    String skuCode;
    BigDecimal skuPrice;
    String skuImage;
    List<SkuAttributeResponse> skuAttributes;
    Integer rating;
    String comment;
    Boolean isApproved;
    LocalDateTime createdAt;
}
