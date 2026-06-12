package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAttributeRequest {
    @Size(max = 100, message = "Tên thuộc tính không được vượt quá 100 ký tự")
    String name;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    String description;

    @Valid
    List<AttributeValueUpdateRequest> values;
}
