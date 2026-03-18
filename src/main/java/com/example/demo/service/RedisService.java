package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "BLACKLIST:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public void blackListToken(String token, long expirationMillis){
        // Lưu key vào Redis với thời gian sống bằng đúng thời gian còn lại của Token
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "true", // Value không quan trọng, chủ yếu check key tồn tại
                expirationMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isTokenBlacklisted(String token){
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    public void saveRefreshTokenToRedis(String token, long expirationTimeInSeconds){
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + token,
                "active", expirationTimeInSeconds,
                TimeUnit.SECONDS);
    }

    public void deleteRefreshToken(String token) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
    }
}
