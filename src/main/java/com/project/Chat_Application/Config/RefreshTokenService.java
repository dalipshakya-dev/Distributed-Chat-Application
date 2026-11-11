package com.project.Chat_Application.Config;

import com.project.Chat_Application.Exceptions.InvalidRefreshToken;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
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
        String username = stringRedisTemplate.opsForValue().get(key);

        if (username == null) {
            throw new InvalidRefreshToken("Your refresh token is invalid!");
        }

        stringRedisTemplate.delete(key);
    }
}