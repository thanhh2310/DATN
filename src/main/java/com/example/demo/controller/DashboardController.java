package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard") // Bảo mật tuyệt đối
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // 1. Lấy 4 chỉ số tổng quan trên cùng
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/overview")
    public ApiResponse<?> getOverview() {
        return ApiResponse.builder()
                .code(200)
                .message("Lấy chỉ số tổng quan thành công")
                .data(dashboardService.getOverviewStats())
                .build();
    }

    // 2. Lấy data vẽ biểu đồ đường (Mặc định 30 ngày)
    @GetMapping("/revenue-chart")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<?> getRevenueChart(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.builder()
                .code(200)
                .message("Lấy biểu đồ doanh thu thành công")
                .data(dashboardService.getRevenueChart(days))
                .build();
    }

    // 3. Lấy Top sản phẩm bán chạy (Mặc định Top 10)
    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<?> getTopProducts(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.builder()
                .code(200)
                .message("Lấy top sản phẩm thành công")
                .data(dashboardService.getTopProducts(limit))
                .build();
    }

    // 4. Lấy cơ cấu doanh thu theo Danh mục (vẽ biểu đồ tròn)
    @GetMapping("/category-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<?> getCategoryRevenue() {
        return ApiResponse.builder()
                .code(200)
                .message("Lấy doanh thu theo danh mục thành công")
                .data(dashboardService.getCategoryRevenue())
                .build();
    }
}
