package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    String name;

    @NotBlank(message = "Slug không được để trống")
    @Size(max = 255, message = "Slug không được vượt quá 255 ký tự")
    String slug;

//    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    String description;

    @Min(value = 0, message = "Giá gốc phải lớn hơn hoặc bằng 0")
    BigDecimal basePrice;

    Integer categoryId;
    Integer brandId;
    Boolean isActive;

    List<String> imageUrls;
    @Valid
    List<ProductSpecRequest> specs;

}
