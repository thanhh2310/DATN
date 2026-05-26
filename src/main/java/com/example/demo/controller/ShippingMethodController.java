package com.example.demo.controller;

import com.example.demo.dto.request.ShippingMethodRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ShippingMethodResponse;
import com.example.demo.service.ShippingMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-methods")
@RequiredArgsConstructor
public class ShippingMethodController {

    private final ShippingMethodService shippingMethodService;

    // ================= CREATE =================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ShippingMethodResponse> create(@Valid @RequestBody ShippingMethodRequest request) {
        return ApiResponse.<ShippingMethodResponse>builder()
                .code(200)
                .message("Create shipping method success")
                .data(shippingMethodService.create(request))
                .build();
    }

    // ================= UPDATE =================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ShippingMethodResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody ShippingMethodRequest request
    ) {
        return ApiResponse.<ShippingMethodResponse>builder()
                .code(200)
                .message("Update shipping method success")
                .data(shippingMethodService.update(id, request))
                .build();
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        shippingMethodService.delete(id);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete shipping method success")
                .build();
    }

    // ================= GET ALL =================
    @GetMapping
    public ApiResponse<List<ShippingMethodResponse>> getAll() {
        return ApiResponse.<List<ShippingMethodResponse>>builder()
                .code(200)
                .message("Get shipping methods success")
                .data(shippingMethodService.getAll())
                .build();
    }

    // ================= GET BY ID =================
    @GetMapping("/{id}")
    public ApiResponse<ShippingMethodResponse> getById(@PathVariable Integer id) {
        return ApiResponse.<ShippingMethodResponse>builder()
                .code(200)
                .message("Get shipping method success")
                .data(shippingMethodService.getById(id))
                .build();
    }
}
