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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
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
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> confirmOrder(@PathVariable Integer orderId) {
        orderService.confirmOrder(orderId);

        return ApiResponse.builder()
                .code(200)
                .message("Xác nhận đơn hàng thành công")
                .build();
    }

    // 2. ADMIN/NHÂN VIÊN XÁC NHẬN GIAO THÀNH CÔNG (Thay thế complete-cod)
    @PutMapping("/{orderId}/deliver")
    @PreAuthorize("hasRole('ADMIN')") // Bạn có thể thêm 'STAFF' hoặc 'SHIPPER' nếu có role này
    public ApiResponse<?> deliverOrder(@PathVariable Integer orderId) {
        orderService.deliverOrder(orderId);

        return ApiResponse.builder()
                .code(200)
                .message("Cập nhật trạng thái giao hàng thành công!")
                .build();
    }

    // 3. HỦY ĐƠN VÀ XỬ LÝ HOÀN TIỀN/HOÀN KHO
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ApiResponse<?> cancelOrder(@PathVariable Integer orderId) {

        // --- ĐOẠN CODE TEST LOG BẰNG SLF4J ---
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            log.info("=== DEBUG SPRING SECURITY ===");
            log.info("Username/Email đang gọi API: {}", authentication.getName());

            // In trực tiếp Collection
            log.info("Tất cả quyền (Authorities Object): {}", authentication.getAuthorities());

            // Map sang String list để nhìn cho rõ (Cách chuẩn nhất)
            List<String> roles = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .toList();
            log.info("Danh sách Roles dạng chuỗi: {}", roles);
            log.info("=============================");
        } else {
            log.warn("CẢNH BÁO: Authentication đang bị NULL (User chưa đăng nhập hoặc mất Token!)");
        }
        // -------------------------------------

        Integer currentUserId = helper.getCurrentUserId();

        // Kiểm tra xem người gọi API có phải là ADMIN không dựa vào Spring Security Context
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        log.info("Kết quả biến isAdmin: {}", isAdmin);

        // Truyền thông tin xuống Service để check quyền
        orderService.cancelOrder(orderId, currentUserId, isAdmin);

        return ApiResponse.builder()
                .code(200)
                .message("Hủy đơn hàng thành công!")
                .build();
    }

    @PutMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> shipOrder(@PathVariable Integer orderId) {
        orderService.shipOrder(orderId);
        return ApiResponse.builder()
                .code(200)
                .message("Đơn hàng đã bắt đầu vận chuyển!")
                .build();
    }

    // 4. ADMIN LẤY TẤT CẢ ĐƠN HÀNG (CÓ PHÂN TRANG)
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
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
