package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password;

    @NotBlank(message = "First name cannot be blank")
    String firstName;

    @NotBlank(message = "Last name cannot be blank")
    String lastName;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^\\d{10,11}$", message = "Phone number must be 10-11 digits")
    String phoneNumber;
}
