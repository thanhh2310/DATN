package com.example.demo.controller;

import com.example.demo.dto.request.AttributeCreationRequest;
import com.example.demo.dto.request.UpdateAttributeRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.AttributeResponse;
import com.example.demo.service.AttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeController {
    private final AttributeService attributeService;

    @GetMapping
    public ApiResponse<List<AttributeResponse>> getAllAttributes() {
        return ApiResponse.<List<AttributeResponse>>builder()
                .code(200)
                .message("Lấy danh sách thuộc tính thành công")
                .data(attributeService.getAllAttribute())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AttributeResponse> getAttributeById(@PathVariable Integer id) {
        return ApiResponse.<AttributeResponse>builder()
                .code(200)
                .message("Lấy thuộc tính thành công")
                .data(attributeService.getById(id))
                .build();
    }

    @PostMapping
    public ApiResponse<AttributeResponse> createAttribute(
            @Valid @RequestBody AttributeCreationRequest request) {
        return ApiResponse.<AttributeResponse>builder()
                .code(200)
                .message("Tạo thuộc tính thành công")
                .data(attributeService.createAttribute(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AttributeResponse> updateAttribute(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateAttributeRequest request) {
        return ApiResponse.<AttributeResponse>builder()
                .code(200)
                .message("Cập nhật thuộc tính thành công")
                .data(attributeService.updateAttribute(request, id))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAttribute(@PathVariable Integer id) {
        attributeService.deleteAttribute(id);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Xóa thuộc tính thành công")
                .build();
    }
}