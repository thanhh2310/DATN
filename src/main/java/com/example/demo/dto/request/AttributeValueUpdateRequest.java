package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "Value must be <= 100 characters")
    String value;

    @Size(max = 500, message = "Description must be <= 500 characters")
    String description;
}
