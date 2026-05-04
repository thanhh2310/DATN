package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttributeValueUpdateRequest {
    Integer id;
    @NotBlank(message = "Value cannot be empty")
    String value;
    String description;
}
