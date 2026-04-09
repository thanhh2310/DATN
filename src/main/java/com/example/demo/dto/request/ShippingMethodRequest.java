package com.example.demo.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShippingMethodRequest {

    @NotBlank(message = "Name must not be blank")
    @Size(max = 100, message = "Name must be <= 100 characters")
    String name;

    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost must be >= 0")
    BigDecimal cost;

    @Size(max = 50, message = "Estimated delivery must be <= 50 characters")
    String estimatedDeliveryDays;
}