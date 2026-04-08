package com.example.demo.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSpecRequest {
    Integer attributeId; // Ví dụ ID của thuộc tính "Hệ điều hành"
    String value;        // Ví dụ: "iOS 17"
}