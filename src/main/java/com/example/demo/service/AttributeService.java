package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.AttributeCreationRequest;
import com.example.demo.dto.request.AttributeCreationRequest.AttributeValueCreationRequest;
import com.example.demo.dto.request.AttributeValueUpdateRequest;
import com.example.demo.dto.request.UpdateAttributeRequest;
import com.example.demo.dto.response.AttributeResponse;
import com.example.demo.mapper.AttributeMapper;
import com.example.demo.model.Attribute;
import com.example.demo.model.AttributeValue;
import com.example.demo.repository.AttributeRepository;
import com.example.demo.repository.AttributeValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeService {
    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeMapper attributeMapper;

    @Transactional(readOnly = true)
    public List<AttributeResponse> getAllAttribute(){
        List<Attribute> attribute = attributeRepository.findAll();
        return attribute.stream()
                .map(attributeMapper::attributeToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttributeResponse getById(Integer id){
        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(()-> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND));
        return attributeMapper.attributeToResponse(attribute);
    }

    @Transactional
    public AttributeResponse createAttribute(AttributeCreationRequest request){
        String attributeName = normalizeRequiredValue(request.getName());
        validateUniqueValueNames(request.getValues());

        if(attributeRepository.existsByNameIgnoreCase(attributeName)){
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_ALREADY_EXISTED);
        }

        // BƯỚC 1: Lưu Cha trước -> Lấy ID chắc chắn
        Attribute attribute = attributeMapper.requestToAttribute(request);
        attribute.setName(attributeName);
        attribute.setDescription(normalizeOptionalValue(request.getDescription()));
        // Xóa dòng setValues nếu mapper lỡ map values vào
        attribute.setValues(null);

        // Save lần 1: Để DB sinh ra ID cho thằng Cha
        Attribute savedAttribute = attributeRepository.save(attribute);

        // BƯỚC 2: Tạo con và gắn ID cha vừa sinh ra vào
        if(request.getValues() != null && !request.getValues().isEmpty()){
            List<AttributeValue> values = request.getValues().stream()
                    .map(valReq -> AttributeValue.builder()
                            .value(normalizeRequiredValue(valReq.getValue()))
                            .description(normalizeOptionalValue(valReq.getDescription()))
                            .attribute(savedAttribute) // 👉 Chắc chắn ID không null
                            .build())
                    .collect(Collectors.toList());

            savedAttribute.setValues(values);
            // Save lần 2: Để Hibernate lưu đám con
            attributeRepository.save(savedAttribute);
        }

        return attributeMapper.attributeToResponse(savedAttribute);
    }

    @Transactional
    public AttributeResponse updateAttribute(UpdateAttributeRequest request, Integer id){
        // tim thang can update
        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(()-> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND));

        //update cac thuoc tinh co ban
        // Validate trùng tên (Nếu đổi tên thì phải check xem tên mới có trùng ai không)
        if (request.getName() != null) {
            String normalizedName = normalizeRequiredValue(request.getName());
            if(!normalizedName.equalsIgnoreCase(attribute.getName())
                    && attributeRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)){
                throw new WebErrorConfig(ErrorCode.ATTRIBUTE_ALREADY_EXISTED);
            }
            attribute.setName(normalizedName);
        }
        if(request.getDescription() != null) attribute.setDescription(normalizeOptionalValue(request.getDescription()));

        // xu ly values
        if(request.getValues() != null && !request.getValues().isEmpty()){
            validateUniqueValueNames(request.getValues());

            // thang attribute dang co 1 list cac value
            // lay list value do ra
            List<AttributeValue> currentValues = attribute.getValues();

            // Biến List hiện tại thành Map<ID, AttributeValue>
            Map<Integer, AttributeValue> currentMap = currentValues.stream()
                    .collect(Collectors.toMap(AttributeValue::getId, val -> val));

            // Dùng Set để lưu lại những ID có trong Request (để tí nữa tính toán việc xóa)
            Set<Integer> requestIds = new HashSet<>();

            //duyet qua tung yeu cau thay doi ma nguoi dung gui
            for (AttributeValueUpdateRequest valReq : request.getValues()){
                //neu id khac null thi yeu cua thay doi attribute-value bang 1 attribute-valu da ton tai
                if(valReq.getId() != null){
                    AttributeValue existing = currentMap.get(valReq.getId());

                    if (existing == null) {
                        throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
                    }

                    if (valReq.getValue() == null || valReq.getValue().isBlank()) {
                        throw new WebErrorConfig(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                    }

                    String normalizedValue = normalizeRequiredValue(valReq.getValue());
                    if (attributeValueRepository.existsByAttributeIdAndValueIgnoreCaseAndIdNot(
                            attribute.getId(),
                            normalizedValue,
                            existing.getId()
                    )) {
                        throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_ALREADY_EXISTED);
                    }

                    existing.setValue(normalizedValue);
                    existing.setDescription(normalizeOptionalValue(valReq.getDescription()));
                    requestIds.add(valReq.getId());
                }else {
                    String normalizedValue = normalizeRequiredValue(valReq.getValue());
                    if (attributeValueRepository.existsByAttributeIdAndValueIgnoreCase(attribute.getId(), normalizedValue)) {
                        throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_ALREADY_EXISTED);
                    }

                    //neu id = null thi them 1 attribute-value moi
                    AttributeValue newVal = AttributeValue.builder()
                            .value(normalizedValue)
                            .description(normalizeOptionalValue(valReq.getDescription()))
                            .attribute(attribute)
                            .build();
                    currentValues.add(newVal);
                }
            }
            List<AttributeValue> valuesToRemove = currentValues.stream()
                    .filter(val -> val.getId() != null && !requestIds.contains(val.getId()))
                    .toList();

            for (AttributeValue valueToRemove : valuesToRemove) {
                if (isAttributeValueInUse(valueToRemove.getId())) {
                    throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_IN_USE);
                }
            }

            currentValues.removeAll(valuesToRemove);
        }


        attributeRepository.save(attribute);

        return attributeMapper.attributeToResponse(attribute);
    }

    @Transactional()
    public void deleteAttribute(Integer id){
        if (!attributeRepository.existsById(id)) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND);
        }

        if (attributeValueRepository.isAttributeUsedInProductSpecs(id)
                || attributeValueRepository.isAttributeUsedInSkuValues(id)) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_IN_USE);
        }

        attributeRepository.deleteById(id);
    }

    private void validateUniqueValueNames(List<?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        Set<String> seen = new HashSet<>();
        for (Object value : values) {
            String rawValue;
            if (value instanceof AttributeValueCreationRequest creationRequest) {
                rawValue = creationRequest.getValue();
            } else if (value instanceof AttributeValueUpdateRequest updateRequest) {
                rawValue = updateRequest.getValue();
            } else {
                throw new WebErrorConfig(ErrorCode.INVALID_ATTRIBUTE_VALUE);
            }

            String normalizedValue = normalizeRequiredValue(rawValue).toLowerCase();
            if (!seen.add(normalizedValue)) {
                throw new WebErrorConfig(ErrorCode.DUPLICATE_ATTRIBUTE_VALUE_ID_IN_REQUEST);
            }
        }
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

    private boolean isAttributeValueInUse(Integer attributeValueId) {
        return attributeValueRepository.isUsedInProductSpecs(attributeValueId)
                || attributeValueRepository.isUsedInSkuValues(attributeValueId);
    }

}
