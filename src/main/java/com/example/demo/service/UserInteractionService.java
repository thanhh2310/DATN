package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.UserInteractionRequest;
import com.example.demo.dto.response.UserInteractionResponse;
import com.example.demo.model.Product;
import com.example.demo.model.UserInteraction;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserInteractionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInteractionService {

    private final UserInteractionRepository userInteractionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserInteractionResponse trackInteraction(UserInteractionRequest request, Integer userId) {
        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND));
        }

        UserInteraction.InteractionType interactionType;
        try {
            interactionType = UserInteraction.InteractionType.valueOf(request.getInteractionType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WebErrorConfig(ErrorCode.INVALID_INTERACTION_TYPE);
        }

        Float weight = getInteractionWeight(interactionType);

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "guest_" + java.util.UUID.randomUUID().toString();
        }

        UserInteraction interaction = UserInteraction.builder()
                .user(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .sessionId(sessionId)
                .product(product)
                .interactionType(interactionType)
                .interactionWeight(weight)
                .searchKeyword(request.getSearchKeyword())
                .build();

        userInteractionRepository.save(interaction);

        return mapToResponse(interaction);
    }

    @Transactional(readOnly = true)
    public List<UserInteractionResponse> getUserHistory(Integer userId) {
        List<UserInteraction> interactions = userInteractionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return interactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserInteractionResponse> getSessionHistory(String sessionId) {
        List<UserInteraction> interactions = userInteractionRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        return interactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Integer, Double> getRecommendedProductScores(Integer userId) {
        List<Object[]> results = userInteractionRepository.getProductScoresByUserId(userId);
        
        Map<Integer, Double> scores = new HashMap<>();
        for (Object[] row : results) {
            Integer productId = (Integer) row[0];
            Double score = (Double) row[1];
            scores.put(productId, score);
        }
        
        return scores;
    }

    private Float getInteractionWeight(UserInteraction.InteractionType type) {
        return switch (type) {
            case PURCHASE -> 5.0f;
            case ADD_TO_CART -> 3.0f;
            case VIEW -> 1.0f;
            case SEARCH -> 0.5f;
        };
    }

    private UserInteractionResponse mapToResponse(UserInteraction interaction) {
        return UserInteractionResponse.builder()
                .id(interaction.getId())
                .userId(interaction.getUser() != null ? interaction.getUser().getId() : null)
                .sessionId(interaction.getSessionId())
                .productId(interaction.getProduct() != null ? interaction.getProduct().getId() : null)
                .productName(interaction.getProduct() != null ? interaction.getProduct().getName() : null)
                .interactionType(interaction.getInteractionType().name())
                .interactionWeight(interaction.getInteractionWeight())
                .searchKeyword(interaction.getSearchKeyword())
                .createdAt(interaction.getCreatedAt())
                .build();
    }

    public void trackPurchase(Integer userId, Integer productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        UserInteraction interaction = UserInteraction.builder()
                .user(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .sessionId(userId != null ? "user_" + userId : "unknown")
                .product(product)
                .interactionType(UserInteraction.InteractionType.PURCHASE)
                .interactionWeight(5.0f)
                .build();

        userInteractionRepository.save(interaction);
    }

    public void trackAddToCart(Integer userId, String sessionId, Integer productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        String effectiveSessionId = sessionId;
        if (effectiveSessionId == null || effectiveSessionId.isBlank()) {
            effectiveSessionId = userId != null ? "user_" + userId : "guest_" + java.util.UUID.randomUUID();
        }

        UserInteraction interaction = UserInteraction.builder()
                .user(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .sessionId(effectiveSessionId)
                .product(product)
                .interactionType(UserInteraction.InteractionType.ADD_TO_CART)
                .interactionWeight(3.0f)
                .build();

        userInteractionRepository.save(interaction);
    }
}
