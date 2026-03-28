package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
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

    public void saveRefreshTokenToRedis(String email, String token, long expirationTimeInSeconds){
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + email + ":" + token,
                "active", expirationTimeInSeconds,
                TimeUnit.SECONDS);
    }

    public void deleteRefreshToken(String email, String token) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email + ":" + token);
    }

    public void deleteAllRefreshTokensOfUser(String email) {
        // Tìm tất cả các key có chứa email của user này
        Set<String> keys = redisTemplate.keys(REFRESH_TOKEN_PREFIX + email + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
