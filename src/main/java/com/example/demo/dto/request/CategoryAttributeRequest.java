package com.example.demo.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CategoryAttributeRequest {
    Integer categoryId;
    Integer attributeId;
    Boolean isRequired = false;
    Boolean isFilterable = false;
}
