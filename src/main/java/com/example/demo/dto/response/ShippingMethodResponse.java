package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShippingMethodResponse {

    Integer id;
    String name;
    BigDecimal cost;
    String estimatedDeliveryDays;
}