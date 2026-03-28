package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Confirmation password is required")
    private String confirmationPassword;
}
