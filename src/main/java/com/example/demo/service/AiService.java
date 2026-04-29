package com.example.demo.service;

import com.example.demo.dto.request.ChatRequest;
import com.example.demo.dto.request.RecommendRequest;
import com.example.demo.dto.response.ChatResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:5001/api/ai}")
    private String aiServiceUrl;

    public ChatResponse processChat(ChatRequest request) {
        String url = aiServiceUrl + "/chat";
        try {
            HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(request);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && "success".equals(body.get("status"))) {
                String reply = (String) body.get("reply");
                List<Integer> productIds = (List<Integer>) body.get("product_ids");

                List<ProductResponse> products = fetchProductsByIds(productIds);

                ChatResponse chatResponse = new ChatResponse();
                chatResponse.setStatus("success");
                chatResponse.setReply(reply);
                chatResponse.setProducts(products);
                return chatResponse;
            }
        } catch (Exception e) {
            log.error("Lỗi khi kết nối Python AI Chatbot: ", e);
        }

        ChatResponse fallback = new ChatResponse();
        fallback.setStatus("error");
        fallback.setReply("Hệ thống chatbot đang bận, xin quý khách quay lại sau.");
        fallback.setProducts(Collections.emptyList());
        return fallback;
    }

    public List<ProductResponse> getRecommendations(RecommendRequest request) {
        String url = aiServiceUrl + "/recommend";
        try {
            HttpEntity<RecommendRequest> requestEntity = new HttpEntity<>(request);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && "success".equals(body.get("status"))) {
                List<Integer> productIds = (List<Integer>) body.get("product_ids");
                return fetchProductsByIds(productIds);
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy Recommendations: ", e);
        }
        return Collections.emptyList();
    }

    private List<ProductResponse> fetchProductsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        
        List<Product> products = productRepository.findAllById(ids);
        
        // Sắp xếp lại danh sách product trả về theo đúng thứ tự ID mà AI recommend
        List<ProductResponse> sortedProducts = new ArrayList<>();
        for (Integer id : ids) {
            products.stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .ifPresent(p -> sortedProducts.add(productMapper.toProductResponse(p)));
        }
        
        return sortedProducts;
    }
}
