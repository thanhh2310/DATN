package com.example.demo.controller;

import com.example.demo.dto.request.CouponRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CouponResponse;
import com.example.demo.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ================= CREATE =================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.<CouponResponse>builder()
                .code(200)
                .message("Create coupon success")
                .data(couponService.create(request))
                .build();
    }

    // ================= UPDATE =================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<CouponResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody CouponRequest request
    ) {
        return ApiResponse.<CouponResponse>builder()
                .code(200)
                .message("Update coupon success")
                .data(couponService.update(id, request))
                .build();
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        couponService.delete(id);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete coupon success")
                .build();
    }

    // ================= GET ALL =================
    @GetMapping
    public ApiResponse<List<CouponResponse>> getAll() {
        return ApiResponse.<List<CouponResponse>>builder()
                .code(200)
                .message("Get coupons success")
                .data(couponService.getAll())
                .build();
    }
}
