package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkuResponse {
    Integer id;
    String skuCode;
    BigDecimal price;
    Integer stockQuantity;
    String imageUrl;
    Boolean isActive;

    // Trả về danh sách giá trị để Frontend in ra màn hình. VD: ["Màu Đen", "256GB"]
    List<SkuAttributeResponse> attributeValues;
}