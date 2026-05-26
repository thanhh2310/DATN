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
import org.springframework.security.core.context.SecurityContextHolder;
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

    // 1. ADMIN XÁC NHẬN ĐƠN (PENDING -> PROCESSING)
    @PutMapping("/{orderId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<?> confirmOrder(@PathVariable Integer orderId) {
        orderService.confirmOrder(orderId);

        return ApiResponse.builder()
                .code(200)
                .message("Xác nhận đơn hàng thành công")
                .build();
    }

    // 2. ADMIN/NHÂN VIÊN XÁC NHẬN GIAO THÀNH CÔNG (Thay thế complete-cod)
    @PutMapping("/{orderId}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<?> deliverOrder(@PathVariable Integer orderId) {
        orderService.deliverOrder(orderId);

        return ApiResponse.builder()
                .code(200)
                .message("Cập nhật trạng thái giao hàng thành công!")
                .build();
    }

    // 3. HỦY ĐƠN VÀ XỬ LÝ HOÀN TIỀN/HOÀN KHO
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public ApiResponse<?> cancelOrder(@PathVariable Integer orderId) {
        Integer currentUserId = helper.getCurrentUserId();

        // Kiểm tra xem người gọi API có phải là ADMIN/STAFF không dựa vào Spring Security Context
        boolean isManager = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        // Truyền thông tin xuống Service để check quyền
        orderService.cancelOrder(orderId, currentUserId, isManager);

        return ApiResponse.builder()
                .code(200)
                .message("Hủy đơn hàng thành công!")
                .build();
    }

    @PutMapping("/{orderId}/ship")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<?> shipOrder(@PathVariable Integer orderId) {
        orderService.shipOrder(orderId);
        return ApiResponse.builder()
                .code(200)
                .message("Đơn hàng đã bắt đầu vận chuyển!")
                .build();
    }

    // 4. ADMIN LẤY TẤT CẢ ĐƠN HÀNG (CÓ PHÂN TRANG)
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<PageResponse<OrderHistoryResponse>> getAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<OrderHistoryResponse>>builder()
                .code(200)
                .message("Lấy danh sách toàn bộ đơn hàng thành công")
                .data(orderService.getAllOrdersPaginated(page, size))
                .build();
    }

}
