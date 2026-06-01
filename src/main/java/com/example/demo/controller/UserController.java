package com.example.demo.controller;

import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.request.UserCreationRequest;
import com.example.demo.dto.request.UserUpdateRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // API CHO CLIENT

    @GetMapping("/my-profile")
    public ApiResponse<ProfileResponse> getMyProfile() {
        ProfileResponse response = userService.getMyProfile();
        return ApiResponse.<ProfileResponse>builder()
                .code(200)
                .message("Lấy thông tin cá nhân thành công")
                .data(response)
                .build();
    }

    @PutMapping("/my-profile")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(request);
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Cập nhật thông tin cá nhân thành công")
                .data(response)
                .build();
    }

    // API CHO QUẢN TRỊ VIÊN (ADMIN)

    @GetMapping
    @PreAuthorize("hasRole('ADMIN', 'STAFF')")
    public ApiResponse<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageResponse<UserResponse> response = userService.getAllUser(pageNumber, pageSize);
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .code(200)
                .message("Lấy danh sách người dùng thành công")
                .data(response)
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreationRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.<UserResponse>builder()
                .code(201) // Mã code nội bộ (business code)
                .message("Tạo mới tài khoản thành công")
                .data(response)
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserResponse response = userService.updateUser(request, id);
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Cập nhật thông tin tài khoản thành công")
                .data(response)
                .build();
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> toggleUserStatus(@PathVariable Integer id) {
        userService.toggleUserStatus(id);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Thay đổi trạng thái tài khoản thành công")
                .data("Thành công")
                .build();
    }
}