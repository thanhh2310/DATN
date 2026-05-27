package com.example.demo.controller;

import com.example.demo.dto.request.BannerRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.BannerResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {
    private final BannerService bannerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<BannerResponse> create(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.<BannerResponse>builder()
                .code(200)
                .message("Tạo banner thành công")
                .data(bannerService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<BannerResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody BannerRequest request
    ) {
        return ApiResponse.<BannerResponse>builder()
                .code(200)
                .message("Cập nhật banner thành công")
                .data(bannerService.update(id, request))
                .build();
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<BannerResponse> toggleStatus(@PathVariable Integer id) {
        return ApiResponse.<BannerResponse>builder()
                .code(200)
                .message("Thay đổi trạng thái banner thành công")
                .data(bannerService.toggleStatus(id))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        bannerService.delete(id);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Xóa banner thành công")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BannerResponse> getById(@PathVariable Integer id) {
        return ApiResponse.<BannerResponse>builder()
                .code(200)
                .message("Lấy banner thành công")
                .data(bannerService.getById(id))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<PageResponse<BannerResponse>> getAll(
            @RequestParam(required = false) String position,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<BannerResponse>>builder()
                .code(200)
                .message("Lấy danh sách banner thành công")
                .data(bannerService.getAll(position, page, size))
                .build();
    }

    @GetMapping("/active")
    public ApiResponse<List<BannerResponse>> getActive(@RequestParam(required = false) String position) {
        return ApiResponse.<List<BannerResponse>>builder()
                .code(200)
                .message("Lấy danh sách banner đang hiển thị thành công")
                .data(bannerService.getActive(position))
                .build();
    }
}
