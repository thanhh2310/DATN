package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentMethodRequest {

    @NotBlank(message = "Name is required")
    String name;

    @NotBlank(message = "Code is required")
    String code;

    Boolean isActive;
}