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

        AttributeValue newValue = AttributeValue.builder()
                .attribute(attribute)
                .value(value)
                .description(description)
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
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND));

        attributeValue.setValue(request.getValue());
        attributeValue.setDescription(request.getDescription());
        attributeValue = attributeValueRepository.save(attributeValue);

        return AttributeValueResponse.builder()
                .id(attributeValue.getId())
                .value(attributeValue.getValue())
                .description(attributeValue.getDescription())
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
