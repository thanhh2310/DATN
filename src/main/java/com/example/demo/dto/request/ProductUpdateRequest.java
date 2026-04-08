package com.example.demo.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductUpdateRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    String name;

    @NotBlank(message = "Slug không được để trống")
    String slug;

    String description;

    @Min(value = 0, message = "Giá gốc phải lớn hơn hoặc bằng 0")
    BigDecimal basePrice;

    Integer categoryId;
    Integer brandId;
    Boolean isActive;

    List<String> imageUrls;
    List<ProductSpecRequest> specs;

    // Tuyệt đối không để List<ProductSkuRequest> ở đây
}