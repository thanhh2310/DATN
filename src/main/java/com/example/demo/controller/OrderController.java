package com.example.demo.controller;

import com.example.demo.dto.request.PlaceOrderRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.OrderHistoryResponse;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.service.Helper;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final Helper helper;

    @PostMapping
    public ApiResponse<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            HttpServletRequest req
    ) {

        return ApiResponse.<OrderResponse>builder()
                .code(200)
                .message("Tạo đơn hàng thành công")
                .data(orderService.placeOrder(helper.getCurrentUserId(), request, req))
                .build();
    }

    @GetMapping("/my-orders")
    public ApiResponse<PageResponse<OrderHistoryResponse>> getMyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderHistoryResponse>>builder()
                .code(200)
                .message("Lấy lịch sử đơn hàng thành công")
                .data(orderService.getMyOrderHistory(helper.getCurrentUserId(), page, size))
                .build();
    }

    @PutMapping("/{orderId}/complete-cod")
    @PreAuthorize("hasRole('ADMIN')") // Phân quyền chỉ Admin/Nhân viên
    public ApiResponse<?> completeCodOrder(@PathVariable Integer orderId) {
        orderService.completeCodOrder(orderId);
        // (Nhớ lưu vào bảng OrderStatusHistory nữa nhé)

        return ApiResponse.builder()
                .code(200)
                .message("Xác nhận đã thu tiền thành công!")
                .build();
    }

    @PutMapping("/{orderId}/refund-wallet")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> refundOrderToWallet(@PathVariable Integer orderId) {
        orderService.refundOrderToWallet(orderId);

        return ApiResponse.builder()
                .code(200)
                .message("Hoàn tiền đơn hàng vào ví thành công")
                .build();
    }
}
