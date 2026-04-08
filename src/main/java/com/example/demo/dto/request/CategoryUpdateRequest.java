package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryUpdateRequest {

    // Cho phép đổi danh mục cha (null nếu muốn chuyển thành danh mục gốc)
    Integer parentId;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    String name;

    @NotBlank(message = "Slug không được để trống")
    @Size(max = 255, message = "Slug không được vượt quá 255 ký tự")
    String slug;

    @Size(max = 255, message = "Đường dẫn ảnh không được vượt quá 255 ký tự")
    String imageUrl;

    // Cho phép ẩn/hiện danh mục
    Boolean isActive;
}