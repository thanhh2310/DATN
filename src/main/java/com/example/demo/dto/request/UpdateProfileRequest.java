package com.example.demo.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    String lastName;
    String firstName;

    @Pattern(regexp = "^\\d{10,11}$", message = "Phone number must be 10-11 digits")
    String phoneNumber;
}
