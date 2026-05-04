package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpecResponse {
    Integer id;
    Integer attributeId;
    String attributeName;
    Integer attributeValueId;
    String attributeValue;
}