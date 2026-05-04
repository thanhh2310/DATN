package com.example.demo.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSpecRequest {
    Integer attributeId;         // ID của thuộc tính (VD: "Hệ điều hành")
    Integer attributeValueId;    // ID của giá trị thuộc tính (VD: "iOS 17")
}