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
public class SkuDetailResponse {
    Integer id;
    String skuCode;
    BigDecimal price;
    Integer stockQuantity;
    Boolean isActive;
    LocalDateTime createdAt;
    List<SkuImageResponse> images;
    List<SkuAttributeResponse> attributeValues;
}
