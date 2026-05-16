package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import com.example.demo.service.PaymentService;
import com.example.demo.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final WalletService walletService;

    @GetMapping("/vnpay-return")
    public ApiResponse<String> vnpayReturn(@RequestParam Map<String, String> allParams) {
        // 1. Kiểm tra chữ ký
        boolean isVerified = paymentService.verifyPayment(allParams);

        if (!isVerified) {
            return ApiResponse.<String>builder()
                    .code(400)
                    .message("Lỗi xác thực: Chữ ký không hợp lệ!")
                    .build();
        }

        // 2. Lấy thông tin giao dịch
        String vnp_ResponseCode = allParams.get("vnp_ResponseCode");
        String vnp_TransactionNo = allParams.get("vnp_TransactionNo"); // Mã GD trên hệ thống VNPAY
        String txnRef = allParams.get("vnp_TxnRef");

        // 3. Xử lý kết quả
        if (walletService.isWalletDepositRef(txnRef)) {
            walletService.completeDeposit(txnRef, "00".equals(vnp_ResponseCode), vnp_TransactionNo);
            if ("00".equals(vnp_ResponseCode)) {
                return ApiResponse.<String>builder()
                        .code(200)
                        .message("Nạp tiền vào ví thành công!")
                        .build();
            }

            return ApiResponse.<String>builder()
                    .code(402)
                    .message("Nạp tiền vào ví thất bại hoặc bị hủy bỏ. Mã lỗi: " + vnp_ResponseCode)
                    .build();
        }

        Integer orderId = parseOrderId(txnRef);
        if ("00".equals(vnp_ResponseCode)) {
            // 👉 Gọi hàm xử lý thành công
            orderService.handlePaymentResult(orderId, true, vnp_TransactionNo);
            return ApiResponse.<String>builder()
                    .code(200)
                    .message("Thanh toán thành công! Đơn hàng #" + orderId + " đã được xác nhận.")
                    .build();
        } else {
            // 👉 Gọi hàm xử lý thất bại
            orderService.handlePaymentResult(orderId, false, vnp_TransactionNo);

            return ApiResponse.<String>builder()
                    .code(402)
                    .message("Thanh toán thất bại hoặc bị hủy bỏ. Mã lỗi: " + vnp_ResponseCode)
                    .build();
        }
    }

    /**
     * API IPN - Webhook dành riêng cho Server VNPAY gọi ngầm
     * Tuyệt đối không dùng ApiResponse ở đây, phải trả về Map để Spring Boot map ra JSON chuẩn của VNPAY
     */
    @GetMapping("/vnpay-ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> allParams) {
        Map<String, String> response = new HashMap<>();

        try {
            // 1. Kiểm tra chữ ký (Checksum)
            boolean isVerified = paymentService.verifyPayment(allParams);
            if (!isVerified) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            // Lấy thông tin từ request
            String vnp_ResponseCode = allParams.get("vnp_ResponseCode");
            String vnp_TransactionNo = allParams.get("vnp_TransactionNo");
            String vnp_Amount = allParams.get("vnp_Amount");
            String txnRef = allParams.get("vnp_TxnRef");

            if (walletService.isWalletDepositRef(txnRef)) {
                BigDecimal amountFromVnPay = new BigDecimal(vnp_Amount).divide(BigDecimal.valueOf(100));
                BigDecimal amountFromDb = walletService.getPendingDepositAmount(txnRef);

                if (amountFromVnPay.compareTo(amountFromDb) != 0) {
                    response.put("RspCode", "04");
                    response.put("Message", "Invalid Amount");
                    return response;
                }

                walletService.completeDeposit(txnRef, "00".equals(vnp_ResponseCode), vnp_TransactionNo);
                response.put("RspCode", "00");
                response.put("Message", "Confirm Success");
                return response;
            }

            Integer orderId = parseOrderId(txnRef);

            // 2. Tìm đơn hàng trong Database
            // Lưu ý: Bạn cần tạo thêm hàm getOrderById trong OrderService nhé
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }

            // 3. Kiểm tra số tiền có khớp không (VNPAY gửi số tiền đã nhân 100)
            long amountFromVnPay = Long.parseLong(vnp_Amount);
            long amountFromDb = order.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();

            if (amountFromVnPay != amountFromDb) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return response;
            }

            // 4. Kiểm tra trạng thái đơn hàng (Đã được cập nhật trước đó chưa?)
            // Tránh tình trạng VNPAY gọi IPN nhiều lần cho 1 đơn hàng đã xử lý
            if (order.getPaymentStatus() == Order.PaymentStatus.PAID ||
                    order.getPaymentStatus() == Order.PaymentStatus.FAILED) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            // 5. Nếu vượt qua mọi cửa ải, tiến hành cập nhật trạng thái
            if ("00".equals(vnp_ResponseCode)) {
                orderService.handlePaymentResult(orderId, true, vnp_TransactionNo);
            } else {
                orderService.handlePaymentResult(orderId, false, vnp_TransactionNo);
            }

            // Phản hồi thành công cho VNPAY để họ ngừng gọi lại
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");

        } catch (Exception e) {
            // Lỗi hệ thống bất ngờ (VD: Lỗi kết nối DB)
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }

        return response;
    }

    private Integer parseOrderId(String txnRef) {
        if (txnRef != null && txnRef.startsWith("ORDER_")) {
            return Integer.parseInt(txnRef.substring("ORDER_".length()));
        }
        return Integer.parseInt(txnRef);
    }
}
