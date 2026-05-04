package com.example.demo.controller;

import com.example.demo.dto.request.AttributeValueUpdateRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.AttributeValueResponse;
import com.example.demo.service.AttributeValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeValueController {
    private final AttributeValueService attributeValueService;

    @PostMapping("/{attributeId}/values")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AttributeValueResponse> addValue(
            @PathVariable Integer attributeId,
            @RequestParam String value,
            @RequestParam(required = false) String description) {
        AttributeValueResponse response =
                attributeValueService.addValue(attributeId, value, description);

        return ApiResponse.<AttributeValueResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Add attribute value successfully")
                .data(response)
                .build();
    }

    @PutMapping("/values/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AttributeValueResponse> updateValue(
            @PathVariable Integer id,
            @RequestBody AttributeValueUpdateRequest request) {
        AttributeValueResponse response =
                attributeValueService.updateValue(id, request);

        return ApiResponse.<AttributeValueResponse>builder()
                .code(200)
                .message("Update attribute value successfully")
                .data(response)
                .build();
    }

    @DeleteMapping("/values/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteValue(@PathVariable Integer id) {
        attributeValueService.deleteValue(id);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete attribute value successfully")
                .data(null)
                .build();
    }
}