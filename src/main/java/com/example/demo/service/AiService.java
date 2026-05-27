package com.example.demo.service;

import com.example.demo.dto.request.ChatRequest;
import com.example.demo.dto.request.RecommendRequest;
import com.example.demo.dto.response.ChatResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.model.ChatbotMessage;
import com.example.demo.model.ChatbotSession;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.ChatbotMessageRepository;
import com.example.demo.repository.ChatbotSessionRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ChatbotSessionRepository chatbotSessionRepository;
    private final ChatbotMessageRepository chatbotMessageRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:5001/api/ai}")
    private String aiServiceUrl;

    public ChatResponse processChat(ChatRequest request) {
        String url = aiServiceUrl + "/chat";
        String sessionId = resolveSessionId(request);
        request.setSessionId(sessionId);
        ChatbotSession session = getOrCreateSession(sessionId, request.getUserId());
        saveMessage(session, ChatbotMessage.SenderType.USER, firstNonBlank(request.getMessage(), request.getQuery()), null);

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(buildChatPayload(request));
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && "success".equals(body.get("status"))) {
                String reply = (String) body.get("reply");
                List<Integer> productIds = extractProductIds(body);
                List<ProductResponse> products = fetchProductsByIds(productIds);
                saveMessage(session, ChatbotMessage.SenderType.BOT, reply, joinProductIds(productIds));

                ChatResponse chatResponse = new ChatResponse();
                chatResponse.setStatus("success");
                chatResponse.setSessionId(sessionId);
                chatResponse.setReply(reply);
                chatResponse.setProducts(products);
                return chatResponse;
            }
        } catch (Exception e) {
            log.error("Lỗi khi kết nối Python AI Chatbot: ", e);
        }

        ChatResponse fallback = new ChatResponse();
        fallback.setStatus("error");
        fallback.setSessionId(sessionId);
        fallback.setReply("Hệ thống chatbot đang bận, xin quý khách quay lại sau.");
        fallback.setProducts(Collections.emptyList());
        saveMessage(session, ChatbotMessage.SenderType.BOT, fallback.getReply(), null);
        return fallback;
    }

    public List<ProductResponse> getRecommendations(RecommendRequest request) {
        if (request.getUserId() == null) {
            return Collections.emptyList();
        }

        String url = aiServiceUrl + "/recommend";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", request.getUserId());
            payload.put("limit", request.getLimit() != null ? request.getLimit() : 6);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && "success".equals(body.get("status"))) {
                return fetchProductsByIds(extractProductIds(body));
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy Recommendations: ", e);
        }
        return Collections.emptyList();
    }

    private Map<String, Object> buildChatPayload(ChatRequest request) {
        Map<String, Object> payload = new HashMap<>();
        String message = firstNonBlank(request.getMessage(), request.getQuery());
        payload.put("message", message);
        payload.put("query", message);
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            payload.put("session_id", request.getSessionId());
        }
        if (request.getUserId() != null) {
            payload.put("user_id", request.getUserId());
        }
        return payload;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second != null ? second.trim() : "";
    }

    private String resolveSessionId(ChatRequest request) {
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return request.getSessionId().trim();
        }
        return UUID.randomUUID().toString();
    }

    private ChatbotSession getOrCreateSession(String sessionId, Integer userId) {
        return chatbotSessionRepository.findById(sessionId)
                .orElseGet(() -> {
                    User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
                    return chatbotSessionRepository.save(ChatbotSession.builder()
                            .id(sessionId)
                            .user(user)
                            .sessionStatus(ChatbotSession.SessionStatus.ACTIVE)
                            .build());
                });
    }

    private void saveMessage(ChatbotSession session, ChatbotMessage.SenderType senderType, String text, String productIds) {
        if (text == null || text.isBlank()) {
            return;
        }

        chatbotMessageRepository.save(ChatbotMessage.builder()
                .session(session)
                .senderType(senderType)
                .messageText(text)
                .retrievedProductIds(productIds)
                .build());
    }

    private String joinProductIds(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return null;
        }
        return productIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @SuppressWarnings("unchecked")
    private List<Integer> extractProductIds(Map<String, Object> body) {
        Object rawIds = body.get("product_ids");
        if (rawIds == null) {
            rawIds = body.get("suggested_product_ids");
        }
        if (!(rawIds instanceof List<?> ids)) {
            return Collections.emptyList();
        }
        List<Integer> productIds = new ArrayList<>();
        for (Object id : ids) {
            if (id instanceof Number number) {
                productIds.add(number.intValue());
            } else if (id instanceof String text) {
                try {
                    productIds.add(Integer.parseInt(text));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed id from AI service.
                }
            }
        }
        return productIds;
    }

    private List<ProductResponse> fetchProductsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        List<Product> products = productRepository.findAvailableProductsByIds(ids);

        List<ProductResponse> sortedProducts = new ArrayList<>();
        for (Integer id : ids) {
            products.stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .map(productMapper::toProductResponse)
                    .map(this::onlyInStockSkus)
                    .ifPresent(sortedProducts::add);
        }

        return sortedProducts;
    }

    private ProductResponse onlyInStockSkus(ProductResponse product) {
        if (product.getSkus() != null) {
            product.setSkus(product.getSkus().stream()
                    .filter(sku -> Boolean.TRUE.equals(sku.getIsActive()))
                    .filter(sku -> sku.getStockQuantity() != null && sku.getStockQuantity() > 0)
                    .collect(Collectors.toList()));
        }
        return product;
    }
}
