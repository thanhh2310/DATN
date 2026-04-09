package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.ShippingMethodRequest;
import com.example.demo.dto.response.ShippingMethodResponse;
import com.example.demo.mapper.ShippingMethodMapper;
import com.example.demo.model.ShippingMethod;
import com.example.demo.repository.ShippingMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingMethodService {

    private final ShippingMethodRepository shippingMethodRepository;
    private final ShippingMethodMapper shippingMethodMapper;

    @Transactional
    public ShippingMethodResponse create(ShippingMethodRequest request) {
        log.info("Creating new shipping method: {}", request.getName());

        if (shippingMethodRepository.existsByName(request.getName())) {
            throw new WebErrorConfig(ErrorCode.SHIPPING_METHOD_ALREADY_EXISTED);
        }

        ShippingMethod method = ShippingMethod.builder()
                .name(request.getName())
                .cost(request.getCost())
                .estimatedDeliveryDays(request.getEstimatedDeliveryDays())
                .build();

        method = shippingMethodRepository.save(method);
        return shippingMethodMapper.toResponse(method);
    }

    @Transactional
    public ShippingMethodResponse update(Integer id, ShippingMethodRequest request) {
        log.info("Updating shipping method ID: {}", id);

        ShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SHIPPING_METHOD_NOT_FOUND));

        if (!method.getName().equals(request.getName()) &&
                shippingMethodRepository.existsByName(request.getName())) {
            throw new WebErrorConfig(ErrorCode.SHIPPING_METHOD_ALREADY_EXISTED);
        }

        method.setName(request.getName());
        method.setCost(request.getCost());
        method.setEstimatedDeliveryDays(request.getEstimatedDeliveryDays());

        method = shippingMethodRepository.save(method);
        return shippingMethodMapper.toResponse(method);
    }

    @Transactional
    public void delete(Integer id) {
        log.info("Deleting shipping method ID: {}", id);

        ShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SHIPPING_METHOD_NOT_FOUND));

        /* * LƯU Ý NGHIỆP VỤ (Góc nhìn Senior):
         * Nếu bảng Orders có khóa ngoại trỏ tới ShippingMethod, việc hard-delete (xóa cứng)
         * sẽ làm lỗi các đơn hàng cũ. Trong thực tế, người ta thường dùng Soft-delete:
         * * method.setIsActive(false);
         * shippingMethodRepository.save(method);
         * * Tạm thời mình vẫn giữ nguyên logic xóa cứng của bạn, nhưng bạn nên cân nhắc nhé.
         */
        shippingMethodRepository.delete(method);
    }

    @Transactional(readOnly = true)
    public List<ShippingMethodResponse> getAll() {
        log.debug("Fetching all shipping methods");

        return shippingMethodRepository.findAll()
                .stream()
                .map(shippingMethodMapper::toResponse)
                .toList();
    }

    // ================= GET BY ID =================
    @Transactional(readOnly = true)
    public ShippingMethodResponse getById(Integer id) {
        log.debug("Fetching shipping method ID: {}", id);

        ShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SHIPPING_METHOD_NOT_FOUND));

        return shippingMethodMapper.toResponse(method);
    }
}