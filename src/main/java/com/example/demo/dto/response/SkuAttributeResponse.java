package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkuAttributeResponse {
    Integer attributeId;   // ID của thuộc tính (VD: 1)
    String attributeName;  // Tên thuộc tính (VD: "Màu sắc")
    Integer valueId;       // ID của giá trị (VD: 10)
    String valueName;      // Tên giá trị (VD: "Xanh")
}
