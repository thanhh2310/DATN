package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSpecRequest {
    @NotNull(message = "ID thuộc tính không được để trống")
    Integer attributeId;         // ID của thuộc tính (VD: "Hệ điều hành")

    @NotNull(message = "ID giá trị thuộc tính không được để trống")
    Integer attributeValueId;    // ID của giá trị thuộc tính (VD: "iOS 17")
}
