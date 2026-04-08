package com.example.demo.controller;

import com.example.demo.dto.request.CategoryCreationRequest;
import com.example.demo.dto.request.CategoryUpdateRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreationRequest request) {
        CategoryResponse data = categoryService.createCategory(request);
        return ApiResponse.<CategoryResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo danh mục thành công")
                .data(data)
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        CategoryResponse data = categoryService.updateCategory(id, request);
        return ApiResponse.<CategoryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật danh mục thành công")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Integer id) {
        CategoryResponse data = categoryService.getCategoryById(id);
        return ApiResponse.<CategoryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin chi tiết danh mục thành công")
                .data(data)
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<CategoryResponse>> getAllCategories(Pageable pageable) {

        PageResponse<CategoryResponse> data = categoryService.getAllCategories(pageable);

        return ApiResponse.<PageResponse<CategoryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách danh mục phân trang thành công")
                .data(data)
                .build();
    }

    // Tách riêng endpoint cho getCategoryTree để không bị trùng lặp với getAll
    @GetMapping("/tree")
    public ApiResponse<List<CategoryResponse>> getCategoryTree() {
        List<CategoryResponse> data = categoryService.getCategoryTree();
        return ApiResponse.<List<CategoryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy cây danh mục thành công")
                .data(data)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa danh mục thành công")
                .data(null)
                .build();
    }
}