package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordRequest {
    @NotBlank(message = "email is required")
    @Email(message = "Invalid email format")
    private String email;
}
