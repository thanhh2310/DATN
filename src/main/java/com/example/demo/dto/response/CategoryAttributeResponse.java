package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryAttributeResponse {
    Integer id;
    Integer categoryId;
    Integer attributeId;
    String attributeName;
    Boolean isRequired;
    Boolean isFilterable;
}
