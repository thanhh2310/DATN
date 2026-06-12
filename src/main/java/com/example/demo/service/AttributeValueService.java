package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.AttributeValueUpdateRequest;
import com.example.demo.dto.response.AttributeValueResponse;
import com.example.demo.model.Attribute;
import com.example.demo.model.AttributeValue;
import com.example.demo.repository.AttributeRepository;
import com.example.demo.repository.AttributeValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeValueService {
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeRepository attributeRepository;

    @Transactional
    public AttributeValueResponse addValue(Integer attributeId, String value, String description) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND));

        String normalizedValue = normalizeRequiredValue(value);
        if (attributeValueRepository.existsByAttributeIdAndValueIgnoreCase(attributeId, normalizedValue)) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_ALREADY_EXISTED);
        }

        AttributeValue newValue = AttributeValue.builder()
                .attribute(attribute)
                .value(normalizedValue)
                .description(normalizeOptionalValue(description))
                .build();

        newValue = attributeValueRepository.save(newValue);
        
        return AttributeValueResponse.builder()
                .id(newValue.getId())
                .value(newValue.getValue())
                .description(newValue.getDescription())
                .build();
    }

    @Transactional
    public AttributeValueResponse updateValue(Integer id, AttributeValueUpdateRequest request) {
        AttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND));

        String normalizedValue = normalizeRequiredValue(request.getValue());
        if (attributeValueRepository.existsByAttributeIdAndValueIgnoreCaseAndIdNot(
                attributeValue.getAttribute().getId(),
                normalizedValue,
                id
        )) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_ALREADY_EXISTED);
        }

        attributeValue.setValue(normalizedValue);
        attributeValue.setDescription(normalizeOptionalValue(request.getDescription()));
        attributeValue = attributeValueRepository.save(attributeValue);

        return AttributeValueResponse.builder()
                .id(attributeValue.getId())
                .value(attributeValue.getValue())
                .description(attributeValue.getDescription())
                .build();
    }

    @Transactional
    public void deleteValue(Integer id) {
        AttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND));

        if (attributeValueRepository.isUsedInProductSpecs(attributeValue.getId())
                || attributeValueRepository.isUsedInSkuValues(attributeValue.getId())) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_IN_USE);
        }

        attributeValueRepository.delete(attributeValue);
    }

    private String normalizeRequiredValue(String value) {
        if (value == null || value.isBlank()) {
            throw new WebErrorConfig(ErrorCode.INVALID_ATTRIBUTE_VALUE);
        }
        return value.trim();
    }

    private String normalizeOptionalValue(String value) {
        return value == null ? null : value.trim();
    }
}
