package com.example.demo.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class RecommendRequest {
    private Integer userId;
    private Integer limit = 6;
    private List<String> contexts;
}
