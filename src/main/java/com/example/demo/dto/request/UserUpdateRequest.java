package com.example.demo.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    String firstName;

    String lastName;

    @Pattern(regexp = "^\\d{10,11}$", message = "Phone number must be 10-11 digits")
    String phoneNumber;

    // Admin có quyền thay đổi Role (Thăng chức/Giáng chức)
    Set<String> roles;

}
