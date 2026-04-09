package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddressResponse {
    Integer id;
    Integer userId;

    String addressLine1;
    String addressLine2;

    String city;
    String state;
    String country;
    String postalCode;

    Boolean isDefault;
    LocalDateTime createdAt;
}
