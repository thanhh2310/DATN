package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String OTP_PREFIX = "VERIFY_CODE:";
    private static final long OTP_EXPIRATION_MINUTES = 5;

    private String generateRandomOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Tạo số từ 100000 -> 999999
        return String.valueOf(otp);
    }

    public String generateAndStoreOtp(String email){
        // gen code to send
        String code = generateRandomOtp();
        // save to redis
        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        return code;
    }

    public boolean validateOtp(String email, String codeToVerify) {
        codeToVerify = codeToVerify.trim();
        String key = OTP_PREFIX + email;
        String rateLimitKey = "OTP_RATE:" + email;
        Object storedCode = redisTemplate.opsForValue().get(key);

        // OTP hết hạn
        if (storedCode == null) {
            throw new WebErrorConfig(ErrorCode.OTP_EXPIRED);
        }

        // So sánh an toàn
        boolean isMatch = MessageDigest.isEqual(
                storedCode.toString().getBytes(),
                codeToVerify.getBytes()
        );

        if (isMatch) {
            redisTemplate.delete(key);
            redisTemplate.delete(rateLimitKey);
            return true;
        }
        // Sai OTP → tăng số lần thử
        Long attempts = redisTemplate.opsForValue().increment(rateLimitKey);

        if (attempts == 1) {
            redisTemplate.expire(rateLimitKey, 5, TimeUnit.MINUTES);
        }

        if (attempts > 3) {
            throw new WebErrorConfig(ErrorCode.OTP_RATE_LIMIT_EXCEEDED);
        }

        return false;
    }

    public void deleteOtp(String gmail){

    }
}
