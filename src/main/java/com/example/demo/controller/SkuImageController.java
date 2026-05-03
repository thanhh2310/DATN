package com.example.demo.controller;

import com.example.demo.dto.request.SkuImageRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.SkuImageResponse;
import com.example.demo.service.SkuImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skus")
@RequiredArgsConstructor
public class SkuImageController {

    private final SkuImageService skuImageService;

    @PostMapping("/{skuId}/images")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkuImageResponse> addImage(
            @PathVariable Integer skuId,
            @Valid @RequestBody SkuImageRequest request
    ) {
        SkuImageResponse response = skuImageService.addImage(skuId, request);

        return ApiResponse.<SkuImageResponse>builder()
                .code(200)
                .message("Thêm ảnh SKU thành công")
                .data(response)
                .build();
    }

    @DeleteMapping("/{skuId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteImage(
            @PathVariable Integer skuId,
            @PathVariable Integer imageId
    ) {
        skuImageService.deleteImage(skuId, imageId);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Xóa ảnh SKU thành công")
                .data(null)
                .build();
    }

    @PutMapping("/{skuId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkuImageResponse> updateImage(
            @PathVariable Integer skuId,
            @PathVariable Integer imageId,
            @Valid @RequestBody SkuImageRequest request
    ) {
        SkuImageResponse response = skuImageService.updateImage(skuId, imageId, request);

        return ApiResponse.<SkuImageResponse>builder()
                .code(200)
                .message("Cập nhật ảnh SKU thành công")
                .data(response)
                .build();
    }

    @GetMapping("/{skuId}/images")
    public ApiResponse<List<SkuImageResponse>> getImages(
            @PathVariable Integer skuId
    ) {
        List<SkuImageResponse> responses = skuImageService.getImages(skuId);

        return ApiResponse.<List<SkuImageResponse>>builder()
                .code(200)
                .message("Lấy danh sách ảnh SKU thành công")
                .data(responses)
                .build();
    }
}
