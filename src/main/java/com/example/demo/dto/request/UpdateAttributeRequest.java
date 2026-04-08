package com.example.demo.dto.request;

import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAttributeRequest {
    String name;
    String description;

    @Valid
    List<AttributeValueUpdateRequest> values;
}
