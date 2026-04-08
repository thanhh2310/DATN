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
public class ProductResponse {

    Integer id;
    String name;
    String slug;
    String description;
    BigDecimal basePrice;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Thay vì trả về nguyên Object Category/Brand, chỉ cần trả ID và Tên cho nhẹ
    Integer categoryId;
    String categoryName;
    Integer brandId;
    String brandName;

    // Danh sách con trỏ đến các DTO độc lập
    List<ImageResponse> images;
    List<SpecResponse> specs;
    List<SkuResponse> skus;
}