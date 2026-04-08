package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrandRequest {
    @NotBlank(message = "Tên thương hiệu không được để trống")
    String name;

    @NotBlank(message = "Slug không được để trống")
    String slug;

    String logoUrl;
    String description;

    @Builder.Default
    Boolean isActive = true;
}