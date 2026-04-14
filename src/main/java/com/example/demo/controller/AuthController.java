package com.example.demo.controller;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.TokenResponse;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Đăng ký thành công. Vui lòng kiểm tra email để lấy mã OTP!")
                .data(null)
                .build();
    }

    @PostMapping("/verify")
    public ApiResponse<String> verify(@Valid @RequestBody VerifyRequest request) {
        authService.verify(request);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Xác thực tài khoản thành công! Bạn đã có thể đăng nhập.")
                .data(null)
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ApiResponse.<TokenResponse>builder()
                .code(200)
                .message("Đăng nhập thành công!")
                .data(tokenResponse)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(authHeader, request);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Đăng xuất thành công!")
                .data(null)
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Yêu cầu thành công. Vui lòng kiểm tra email để nhận mã OTP khôi phục!")
                .data(null)
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Đặt lại mật khẩu thành công! Hãy đăng nhập lại bằng mật khẩu mới.")
                .data(null)
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Đổi mật khẩu thành công! Vui lòng đăng nhập lại trên các thiết bị.")
                .data(null)
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.<TokenResponse>builder()
                .code(200)
                .message("Refresh token success")
                .data(authService.refreshToken(request))
                .build();
    }
}