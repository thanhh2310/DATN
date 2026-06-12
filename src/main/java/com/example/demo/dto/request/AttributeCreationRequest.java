package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttributeCreationRequest {

    @NotBlank(message = "Tên thuộc tính không được để trống")
    @Size(max = 100, message = "Tên thuộc tính không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;

    @Valid
    private List<AttributeValueCreationRequest> values;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttributeValueCreationRequest {
        @NotBlank(message = "Giá trị thuộc tính không được để trống")
        @Size(max = 100, message = "Giá trị thuộc tính không được vượt quá 100 ký tự")
        String value;

        @Size(max = 500, message = "Mô tả giá trị thuộc tính không được vượt quá 500 ký tự")
        String description;
    }
}
