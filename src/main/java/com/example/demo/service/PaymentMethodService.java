package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.PaymentMethodRequest;
import com.example.demo.dto.response.PaymentMethodResponse;
import com.example.demo.mapper.PaymentMethodMapper;
import com.example.demo.model.PaymentMethod;
import com.example.demo.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository repository;
    private final PaymentMethodMapper mapper;

    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest request) {

        if (repository.existsByCode(request.getCode().toUpperCase())) {
            throw new WebErrorConfig(ErrorCode.PAYMENT_METHOD_ALREADY_EXISTED);
        }

        PaymentMethod method = PaymentMethod.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapper.toResponse(repository.save(method));
    }

    @Transactional
    public PaymentMethodResponse update(Integer id, PaymentMethodRequest request) {

        PaymentMethod method = repository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        String newCode = request.getCode().toUpperCase();

        if (!method.getCode().equals(newCode) && repository.existsByCode(newCode)) {
            throw new WebErrorConfig(ErrorCode.PAYMENT_METHOD_ALREADY_EXISTED);
        }

        method.setName(request.getName());
        method.setCode(newCode);

        if (request.getIsActive() != null) {
            method.setIsActive(request.getIsActive());
        }

        return mapper.toResponse(repository.save(method));
    }

    @Transactional
    public void disable(Integer id) {
        PaymentMethod method = repository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        method.setIsActive(false);
        repository.save(method);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getActive() {
        return repository.findByIsActiveTrue()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentMethodResponse getById(Integer id) {
        return mapper.toResponse(
                repository.findById(id)
                        .orElseThrow(() -> new WebErrorConfig(ErrorCode.PAYMENT_METHOD_NOT_FOUND))
        );
    }
}