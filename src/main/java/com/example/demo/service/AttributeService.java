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
        if(attributeRepository.existsByName(request.getName())){
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_ALREADY_EXISTED);
        }

        // BƯỚC 1: Lưu Cha trước -> Lấy ID chắc chắn
        Attribute attribute = attributeMapper.requestToAttribute(request);
        // Xóa dòng setValues nếu mapper lỡ map values vào
        attribute.setValues(null);

        // Save lần 1: Để DB sinh ra ID cho thằng Cha
        Attribute savedAttribute = attributeRepository.save(attribute);

        // BƯỚC 2: Tạo con và gắn ID cha vừa sinh ra vào
        if(request.getValues() != null && !request.getValues().isEmpty()){
            List<AttributeValue> values = request.getValues().stream()
                    .map(valReq -> AttributeValue.builder()
                            .value(valReq.getValue())
                            .description(valReq.getDescription())
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
        if (request.getName() != null && !request.getName().equals(attribute.getName())) {
            if(attributeRepository.existsByName(request.getName())){
                throw new WebErrorConfig(ErrorCode.ATTRIBUTE_ALREADY_EXISTED);
            }
            attribute.setName(request.getName());
        }
        if(request.getDescription() != null) attribute.setDescription(request.getDescription());

        // xu ly values
        if(request.getValues() != null && !request.getValues().isEmpty()){
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

                    existing.setValue(valReq.getValue());
                    existing.setDescription(valReq.getDescription());
                    requestIds.add(valReq.getId());
                }else {
                    //neu id = null thi them 1 attribute-value moi
                    AttributeValue newVal = AttributeValue.builder()
                            .value(valReq.getValue())
                            .description(valReq.getDescription())
                            .attribute(attribute)
                            .build();
                    currentValues.add(newVal);
                }
            }
            currentValues.removeIf(val ->
                    val.getId() != null && !requestIds.contains(val.getId())
            );
        }


        attributeRepository.save(attribute);

        return attributeMapper.attributeToResponse(attribute);
    }

    @Transactional()
    public void deleteAttribute(Integer id){
        if (!attributeRepository.existsById(id)) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND);
        }
        attributeRepository.deleteById(id);
    }
}
