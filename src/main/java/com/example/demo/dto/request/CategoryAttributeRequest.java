package com.example.demo.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CategoryAttributeRequest {
    Integer categoryId;

    @NotNull(message = "Attribute ID không được để trống")
    Integer attributeId;

    Boolean isRequired = false;
    Boolean isFilterable = false;
}
