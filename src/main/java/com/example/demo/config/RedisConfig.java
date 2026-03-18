package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 1. Phải có ConnectionFactory
        template.setConnectionFactory(connectionFactory);

        // 2. Cấu hình Key Serializer (luôn là String)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 3. Cấu hình Value Serializer (dùng JSON để lưu Object)
        // Dùng GenericJackson2JsonRedisSerializer để nó có thể lưu bất kỳ Object nào
        // (Boolean, User, String,...) một cách an toàn.
        template.setValueSerializer(GenericJacksonJsonRedisSerializer.builder().enableUnsafeDefaultTyping().build());
        template.setHashValueSerializer(GenericJacksonJsonRedisSerializer.builder().enableUnsafeDefaultTyping().build());

        return template;
    }
}
