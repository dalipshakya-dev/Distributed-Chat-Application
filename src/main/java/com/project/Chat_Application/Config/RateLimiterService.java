package com.project.Chat_Application.Config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Component
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    public RateLimiterService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - window.toMillis();

        stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        Long currentCount = stringRedisTemplate.opsForZSet().zCard(key);

        if (currentCount != null && currentCount >= maxRequests) {
            return false;
        }

        stringRedisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        stringRedisTemplate.expire(key, window);

        return true;
    }
}