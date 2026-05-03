package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkuImageRequest {

    @NotBlank(message = "URL ảnh không được để trống")
    String imageUrl;

    @Builder.Default
    Boolean isThumbnail = false;

    @Builder.Default
    Integer displayOrder = 0;
}
