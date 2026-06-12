package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class ProductCreationRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    String name;

    @NotBlank(message = "Slug không được để trống")
    @Size(max = 255, message = "Slug không được vượt quá 255 ký tự")
    String slug;

//    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    String description;

    @NotNull(message = "Giá gốc không được để trống")
    @Min(value = 0, message = "Giá gốc phải lớn hơn hoặc bằng 0")
    BigDecimal basePrice;

    @NotNull(message = "ID Danh mục không được để trống")
    Integer categoryId;

    @NotNull(message = "ID Thương hiệu không được để trống")
    Integer brandId;

    // Danh sách URL ảnh sản phẩm (Frontend đã upload lên Cloudinary và trả về link)
//    @NotEmpty(message = "Phải có ít nhất 1 ảnh sản phẩm")
    List<String> imageUrls;

    // Các thông số kỹ thuật (Ví dụ: RAM: 8GB, Màn hình: 14 inch)
    @Valid
    List<ProductSpecRequest> specs;

    // Các biến thể SKU (Ví dụ: Màu Đen - 256GB - Giá 20tr - Tồn kho 10)
    @NotEmpty(message = "Phải có ít nhất 1 SKU (phiên bản sản phẩm)")
    @Valid
    List<ProductSkuRequest> skus;
}
