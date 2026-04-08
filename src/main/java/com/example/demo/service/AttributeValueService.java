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
    public AttributeValueResponse addValue(Integer attributeId, String value) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND));

        AttributeValue newValue = AttributeValue.builder()
                .attribute(attribute)
                .value(value)
                .build();

        newValue = attributeValueRepository.save(newValue);
        
        return AttributeValueResponse.builder()
                .id(newValue.getId())
                .value(newValue.getValue())
                .build();
    }

    @Transactional
    public AttributeValueResponse updateValue(Integer id, AttributeValueUpdateRequest request) {
        AttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND)); // You can add ATTRIBUTE_VALUE_NOT_FOUND to ErrorCode

        attributeValue.setValue(request.getValue());
        attributeValue = attributeValueRepository.save(attributeValue);

        return AttributeValueResponse.builder()
                .id(attributeValue.getId())
                .value(attributeValue.getValue())
                .build();
    }

    @Transactional
    public void deleteValue(Integer id) {
        if (!attributeValueRepository.existsById(id)) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND);
        }
        attributeValueRepository.deleteById(id);
    }
}
