package com.example.demo.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductFilterRequest {
    Integer categoryId;               // Lọc theo danh mục
    Integer brandId;                  // Lọc theo thương hiệu
    BigDecimal minPrice;              // Giá tối thiểu
    BigDecimal maxPrice;              // Giá tối đa
    List<Integer> attributeValueIds;  // Lọc theo giá trị thuộc tính (VD: Màu Đỏ id=1, RAM 8GB id=5)
    String sortBy;                    // Sắp xếp: "price_asc", "price_desc", "newest", "name_asc"...

    @Builder.Default
    int pageNumber = 1;
    @Builder.Default
    int pageSize = 10;
}
