package com.example.demo.controller;

import com.example.demo.dto.request.PaymentMethodRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PaymentMethodResponse;
import com.example.demo.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentMethodResponse> create(
            @Valid @RequestBody PaymentMethodRequest request
    ) {
        return ApiResponse.<PaymentMethodResponse>builder()
                .code(200)
                .message("Create payment method success")
                .data(service.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentMethodResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody PaymentMethodRequest request
    ) {
        return ApiResponse.<PaymentMethodResponse>builder()
                .code(200)
                .message("Update payment method success")
                .data(service.update(id, request))
                .build();
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> disable(@PathVariable Integer id) {
        service.disable(id);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Disable payment method success")
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PaymentMethodResponse>> getAll() {
        return ApiResponse.<List<PaymentMethodResponse>>builder()
                .code(200)
                .message("Get all payment methods success")
                .data(service.getAll())
                .build();
    }

    @GetMapping("/active")
    public ApiResponse<List<PaymentMethodResponse>> getActive() {
        return ApiResponse.<List<PaymentMethodResponse>>builder()
                .code(200)
                .message("Get active payment methods success")
                .data(service.getActive())
                .build();
    }
}