package com.example.demo.controller;

import com.example.demo.dto.request.SkuRequest;
import com.example.demo.dto.request.SkuStockUpdateRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.SkuDetailResponse;
import com.example.demo.service.SkuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkuDetailResponse> createSku(
            @PathVariable Integer productId,
            @Valid @RequestBody SkuRequest request
    ) {
        SkuDetailResponse response = skuService.createSku(productId, request);

        return ApiResponse.<SkuDetailResponse>builder()
                .code(200)
                .message("Thêm SKU thành công")
                .data(response)
                .build();
    }

    @PutMapping("/{skuId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkuDetailResponse> updateSku(
            @PathVariable Integer productId,
            @PathVariable Integer skuId,
            @Valid @RequestBody SkuRequest request
    ) {
        SkuDetailResponse response = skuService.updateSku(skuId, request);

        return ApiResponse.<SkuDetailResponse>builder()
                .code(200)
                .message("Cập nhật SKU thành công")
                .data(response)
                .build();
    }

    @PatchMapping("/{skuId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkuDetailResponse> updateStock(
            @PathVariable Integer productId,
            @PathVariable Integer skuId,
            @Valid @RequestBody SkuStockUpdateRequest request
    ) {
        SkuDetailResponse response = skuService.updateStock(skuId, request);

        return ApiResponse.<SkuDetailResponse>builder()
                .code(200)
                .message("Cập nhật tồn kho thành công")
                .data(response)
                .build();
    }

    @GetMapping("/{skuId}")
    public ApiResponse<SkuDetailResponse> getSku(
            @PathVariable Integer productId,
            @PathVariable Integer skuId
    ) {
        SkuDetailResponse response = skuService.getSkuById(skuId);

        return ApiResponse.<SkuDetailResponse>builder()
                .code(200)
                .message("Lấy thông tin SKU thành công")
                .data(response)
                .build();
    }

    @GetMapping
    public ApiResponse<List<SkuDetailResponse>> getSkusByProduct(
            @PathVariable Integer productId
    ) {
        List<SkuDetailResponse> responses = skuService.getSkusByProductId(productId);

        return ApiResponse.<List<SkuDetailResponse>>builder()
                .code(200)
                .message("Lấy danh sách SKU thành công")
                .data(responses)
                .build();
    }

    @DeleteMapping("/{skuId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteSku(
            @PathVariable Integer productId,
            @PathVariable Integer skuId
    ) {
        skuService.deleteSku(skuId);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Xóa SKU thành công")
                .data(null)
                .build();
    }
}
