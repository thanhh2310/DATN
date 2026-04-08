package com.example.demo.controller;

import com.example.demo.dto.request.BrandRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.BrandResponse;
import com.example.demo.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @PostMapping
    public ApiResponse<BrandResponse> createBrand(
            @Valid @RequestBody BrandRequest request) {
        return ApiResponse.<BrandResponse>builder()
                .code(200)
                .message("Tạo brand thành công")
                .data(brandService.createBrand(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<BrandResponse>> getAllBrands() {
        return ApiResponse.<List<BrandResponse>>builder()
                .code(200)
                .message("Lấy danh sách brand thành công")
                .data(brandService.getAllBrands())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BrandResponse> getBrandById(@PathVariable Integer id) {
        return ApiResponse.<BrandResponse>builder()
                .code(200)
                .message("Lấy brand thành công")
                .data(brandService.getBrandById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<BrandResponse> updateBrand(
            @PathVariable Integer id,
            @Valid @RequestBody BrandRequest request) {
        return ApiResponse.<BrandResponse>builder()
                .code(200)
                .message("Cập nhật brand thành công")
                .data(brandService.updateBrand(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBrand(@PathVariable Integer id) {
        brandService.deleteBrand(id);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Xóa brand thành công")
                .build();
    }
}