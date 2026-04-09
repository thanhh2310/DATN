package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.CouponRequest;
import com.example.demo.dto.response.CouponResponse;
import com.example.demo.mapper.CouponMapper;
import com.example.demo.model.Coupon;
import com.example.demo.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    @Transactional
    public CouponResponse create(CouponRequest request) {
        String code = normalizeCode(request.getCode());
        log.info("Creating coupon: {}", code);

        if (couponRepository.existsByCode(code)) {
            throw new WebErrorConfig(ErrorCode.COUPON_ALREADY_EXISTED);
        }

        validateDate(request.getStartDate(), request.getEndDate());

        Coupon coupon = Coupon.builder()
                .code(code)
                .discountType(request.getDiscountType().toUpperCase())
                .discountValue(request.getDiscountValue())
                .minOrderValue(defaultIfNull(request.getMinOrderValue()))
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(defaultIfNull(request.getIsActive(), true))
                .build();

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse update(Integer id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.COUPON_NOT_FOUND));

        String code = normalizeCode(request.getCode());
        log.info("Updating coupon ID: {}", id);

        if (!coupon.getCode().equals(code) && couponRepository.existsByCode(code)) {
            throw new WebErrorConfig(ErrorCode.COUPON_ALREADY_EXISTED);
        }

        validateDate(request.getStartDate(), request.getEndDate());

        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType().toUpperCase());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(defaultIfNull(request.getMinOrderValue()));
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        if (request.getIsActive() != null) {
            coupon.setIsActive(request.getIsActive());
        }

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public void delete(Integer id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.COUPON_NOT_FOUND));

        log.info("Soft deleting coupon: {}", coupon.getCode());

        coupon.setIsActive(false);
        couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAll() {
        return couponRepository.findAll()
                .stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Coupon getValidCoupon(String rawCode, BigDecimal cartTotal) {
        String code = normalizeCode(rawCode);

        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.COUPON_NOT_FOUND));

        validateCoupon(coupon, cartTotal);

        return coupon;
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal total) {

        if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {

            BigDecimal discount = total.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));

            return discount.min(total);
        }

        return coupon.getDiscountValue().min(total);
    }

    @Transactional
    public void increaseUsage(Coupon coupon) {
        if (coupon.getUsageLimit() != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
    }

    private void validateCoupon(Coupon coupon, BigDecimal cartTotal) {

        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new WebErrorConfig(ErrorCode.COUPON_INACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new WebErrorConfig(ErrorCode.COUPON_NOT_STARTED);
        }

        if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
            throw new WebErrorConfig(ErrorCode.COUPON_EXPIRED);
        }

        if (coupon.getUsageLimit() != null &&
                coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new WebErrorConfig(ErrorCode.COUPON_USAGE_EXCEEDED);
        }

        if (cartTotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new WebErrorConfig(ErrorCode.COUPON_NOT_APPLICABLE);
        }
    }

    private void validateDate(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new WebErrorConfig(ErrorCode.INVALID_DATE_RANGE);
        }
    }


    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Boolean defaultIfNull(Boolean value, Boolean defaultVal) {
        return value != null ? value : defaultVal;
    }
}