package com.example.demo.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password;

    @NotBlank(message = "First name is required")
    String firstName;

    @NotBlank(message = "Last name is required")
    String lastName;

    @Pattern(regexp = "^\\d{10,11}$", message = "Phone number must be 10-11 digits")
    String phoneNumber;

    // Admin được quyền chọn Role cho user (ADMIN, STAFF, USER...)
    // Truyền vào dạng String: ["ROLE_ADMIN", "ROLE_STAFF"]
    @NotEmpty(message = "Roles cannot be empty")
    Set<String> roles;

    // Admin có thể chọn kích hoạt luôn hay không
    Boolean isActive = true;
}
