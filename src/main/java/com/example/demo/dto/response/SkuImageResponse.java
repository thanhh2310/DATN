package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkuImageResponse {
    Integer id;
    String imageUrl;
    Boolean isThumbnail;
    Integer displayOrder;
}
