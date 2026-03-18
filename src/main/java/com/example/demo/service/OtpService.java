package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

    public boolean validateOtp(String email, String codeToVerify){
        String key = OTP_PREFIX + email;
        Object storedCode = redisTemplate.opsForValue().get(key);
        if(storedCode != null && storedCode.toString().equals(codeToVerify)){
            // Xác thực thành công, xoá key khỏi Redis
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public void deleteOtp(String gmail){

    }
}
