package com.example.demo.controller;

import com.example.demo.dto.request.CategoryAttributeRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CategoryAttributeResponse;
import com.example.demo.service.CategoryAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryAttributeController {
    private final CategoryAttributeService categoryAttributeService;

    @PostMapping("/{categoryId}/attributes")
    public ApiResponse<CategoryAttributeResponse> assignAttribute(
            @PathVariable Integer categoryId,
            @RequestBody CategoryAttributeRequest request) {
        request.setCategoryId(categoryId);

        CategoryAttributeResponse response =
                categoryAttributeService.assignAttributeToCategory(request);

        return ApiResponse.<CategoryAttributeResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Assign attribute successfully")
                .data(response)
                .build();
    }

    @GetMapping("/{categoryId}/attributes")
    public ApiResponse<List<CategoryAttributeResponse>> getByCategory(
            @PathVariable Integer categoryId) {

        List<CategoryAttributeResponse> responses =
                categoryAttributeService.getAttributesByCategoryId(categoryId);
        return ApiResponse.<List<CategoryAttributeResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get attributes successfully")
                .data(responses)
                .build();
    }

    @PutMapping("/attributes/{id}")
    public ApiResponse<CategoryAttributeResponse> update(
            @PathVariable Integer id,
            @RequestBody CategoryAttributeRequest request) {
        CategoryAttributeResponse response =
                categoryAttributeService.updateCategoryAttributeSettings(id, request);

        return ApiResponse.<CategoryAttributeResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Update category attribute successfully")
                .data(response)
                .build();
    }

    @DeleteMapping("/attributes/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        categoryAttributeService.removeCategoryAttribute(id);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete category attribute successfully")
                .data(null)
                .build();
    }
}