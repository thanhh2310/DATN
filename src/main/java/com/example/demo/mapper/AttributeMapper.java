package com.example.demo.mapper;

import com.example.demo.dto.request.AttributeCreationRequest;
import com.example.demo.dto.response.AttributeResponse;
import com.example.demo.dto.response.AttributeValueResponse;
import com.example.demo.model.Attribute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AttributeMapper {
    public AttributeResponse attributeToResponse(Attribute attribute){
        List<AttributeValueResponse> values = new ArrayList<>();
        if(attribute.getValues() != null){
            values = attribute.getValues().stream()
                    .map(val -> AttributeValueResponse.builder()
                            .id(val.getId())
                            .value(val.getValue())
                            .description(val.getDescription())
                            .build())
                    .collect(Collectors.toList());
        }

        return AttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .description(attribute.getDescription())
                .values(values)
                .build();
    }

    public Attribute requestToAttribute(AttributeCreationRequest request){
        return Attribute.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

    }

}