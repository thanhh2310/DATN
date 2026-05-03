package com.example.demo.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSkuRequest {

    @NotBlank(message = "Mã SKU không được để trống")
    String skuCode;

    @NotNull(message = "Giá SKU không được để trống")
    @Min(value = 0, message = "Giá SKU không được âm")
    BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Tồn kho không được âm")
    Integer stockQuantity;

    List<String> imageUrls; // (Tùy chọn) Danh sách ảnh cho SKU này

    // Danh sách các ID giá trị thuộc tính tạo nên SKU này (VD: ID của "Màu Đen", ID của "256GB")
    // Map vào bảng sku_values
    List<Integer> attributeValueIds;
}