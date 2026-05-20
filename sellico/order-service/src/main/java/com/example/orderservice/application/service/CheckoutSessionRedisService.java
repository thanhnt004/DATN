package com.example.orderservice.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutSessionRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String KEY_PREFIX = "checkout_session:";
    private static final Duration TTL = Duration.ofMinutes(15);

    public void saveSession(String sessionId, Object checkoutSession) {
        String key = KEY_PREFIX + sessionId;
        try {
            redisTemplate.opsForValue().set(key, checkoutSession, TTL);
            log.info("Saved checkout session to Redis successfully. Key: {}", key);
        } catch (Exception e) {
            log.error("Failed to save checkout session to Redis. Key: {}, Error: {}", key, e.getMessage(), e);
            throw new RuntimeException("Failed to save checkout session to Redis", e);
        }
    }

    public Object getSession(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get checkout session from Redis. Key: {}", key, e);
            throw new RuntimeException("Failed to get checkout session from Redis", e);
        }
    }
    public void deleteSession(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        try {
            redisTemplate.delete(key);
            log.info("Deleted checkout session from Redis. Key: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete checkout session. Key: {}", key, e);
        }
    }
}