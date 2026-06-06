package com.example.demo.controller;

import com.example.demo.dto.request.ProductCreationRequest;
import com.example.demo.dto.request.ProductFilterRequest;
import com.example.demo.dto.request.ProductUpdateRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<String> createProduct(
            @Valid @RequestBody ProductCreationRequest request
    ) {
        productService.createProductDetail(request);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Tạo sản phẩm thành công")
                .data(null)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Integer id) {
        ProductResponse response = productService.getProductById(id);

        return ApiResponse.<ProductResponse>builder()
                .code(200)
                .message("Lấy chi tiết sản phẩm thành công")
                .data(response)
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageResponse<ProductResponse> response =
                productService.getAllProducts(pageNumber, pageSize);

        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .code(200)
                .message("Lấy danh sách sản phẩm thành công")
                .data(response)
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);

        return ApiResponse.<ProductResponse>builder()
                .code(200)
                .message("Cập nhật sản phẩm thành công")
                .data(response)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<String> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Xóa sản phẩm thành công")
                .data(null)
                .build();
    }

    // ================= FILTER =================
@GetMapping("/filter")
public ApiResponse<PageResponse<ProductResponse>> filterProducts(
        @RequestParam(required = false) Integer categoryId,
        @RequestParam(required = false) Integer brandId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) List<Integer> attributeValueIds,
        @RequestParam(required = false) String stockFilter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "1") int pageNumber,
        @RequestParam(defaultValue = "10") int pageSize
) {
    ProductFilterRequest request = ProductFilterRequest.builder()
            .categoryId(categoryId)
            .brandId(brandId)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .attributeValueIds(attributeValueIds)
            .stockFilter(stockFilter)
            .sortBy(sortBy)
            .pageNumber(pageNumber)
            .pageSize(pageSize)
            .build();

    return ApiResponse.<PageResponse<ProductResponse>>builder()
            .code(200)
            .message("Lọc sản phẩm thành công")
            .data(productService.filterProducts(request))
            .build();
}

// ================= SEARCH =================
@GetMapping("/search")
public ApiResponse<PageResponse<ProductResponse>> searchProducts(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "1") int pageNumber,
        @RequestParam(defaultValue = "10") int pageSize
) {
    return ApiResponse.<PageResponse<ProductResponse>>builder()
            .code(200)
            .message("Tìm kiếm sản phẩm thành công")
            .data(productService.searchProducts(keyword, pageNumber, pageSize))
            .build();
}

}
