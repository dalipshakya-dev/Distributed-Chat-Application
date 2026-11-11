package com.project.Chat_Application.Config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RefreshTokenService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    public RefreshTokenService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String createRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        String key = "refresh:" + token;
        stringRedisTemplate.opsForValue().set(key, username, REFRESH_TOKEN_TTL);
        return token;
    }

    public String getUsernameFromRefreshToken(String refreshToken) {
        String key = "refresh:" + refreshToken;
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshToken(String refreshToken) {
        String key = "refresh:" + refreshToken;
        stringRedisTemplate.delete(key);
    }
}