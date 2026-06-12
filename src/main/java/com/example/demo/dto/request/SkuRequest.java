package com.example.demo.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SkuRequest {

    @NotBlank(message = "Mã SKU không được để trống")
    @Size(max = 100, message = "Mã SKU không được vượt quá 100 ký tự")
    String skuCode;

    @NotNull(message = "Giá SKU không được để trống")
    @Min(value = 0, message = "Giá SKU không được âm")
    BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Tồn kho không được âm")
    Integer stockQuantity;

    List<String> imageUrls;

    Boolean isActive;

    List<Integer> attributeValueIds;
}
