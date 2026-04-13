package com.example.demo.controller;

import com.example.demo.dto.request.CheckoutPreviewRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CheckoutPreviewResponse;
import com.example.demo.service.CheckoutService;
import com.example.demo.service.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final Helper helper; // Dùng để lấy ID user đang đăng nhập từ Token

    /**
     * API 1: Xem trước hóa đơn (Preview)
     * Tính toán tổng tiền, phí ship, giảm giá dựa trên sự lựa chọn của khách hàng
     */
    @PostMapping("/preview")
    public ApiResponse<CheckoutPreviewResponse> previewCheckout(
            @Valid @RequestBody CheckoutPreviewRequest request
    ) {
        return ApiResponse.<CheckoutPreviewResponse>builder()
                .code(200)
                .message("Tính toán chi tiết hóa đơn thành công")
                .data(checkoutService.preview(helper.getCurrentUserId(), request))
                .build();
    }

    /**
     * API 2: (Gợi ý cho bước tiếp theo) Nút bấm ĐẶT HÀNG
     * Khi FE gọi API Preview xong, khách hàng bấm "Thanh toán", FE sẽ gọi API này
     */
    // @PostMapping("/place-order")
    // public ApiResponse<OrderResponse> placeOrder(
    //         @Valid @RequestBody CheckoutPreviewRequest request
    // ) {
    //     return ApiResponse.<OrderResponse>builder()
    //             .code(200)
    //             .message("Tạo đơn hàng thành công")
    //             .data(orderService.createOrder(helper.getCurrentUserId(), request))
    //             .build();
    // }
}