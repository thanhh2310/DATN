package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpecResponse {
    Integer id;
    String attributeName; // VD: "Hệ điều hành"
    String value;         // VD: "iOS 17"
}