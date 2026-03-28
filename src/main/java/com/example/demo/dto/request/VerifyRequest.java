package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    String email;

    @Size(min = 6,max = 6, message = "maximum 5 numbers")
    @NotBlank(message = "Otp cannot be blank")
    String code;
}
