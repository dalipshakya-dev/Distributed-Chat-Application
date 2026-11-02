package com.project.Chat_Application.Config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class ChatStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamPublisher.class);
    private static final String STREAM_KEY = "chat-messages";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CircuitBreaker redisStreamBreaker;

    public ChatStreamPublisher(@Qualifier("customRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        //inside the constructor initialization
        //It’s created once, when the publisher starts,
        // and the same breaker is reused for every message.
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        this.redisStreamBreaker = CircuitBreaker.of("redisStream", config);
    }

    public void publish(String payloadJson, String correlationId) {
        try {
            redisStreamBreaker.executeRunnable(() -> writeToStream(payloadJson, correlationId));
        } catch (Exception e) {
            publishFallback(payloadJson, correlationId, e);
        }
    }

    private void writeToStream(String payloadJson, String correlationId) {
        Map<String, String> body = new HashMap<>();
        body.put("payload", payloadJson);
        body.put("correlationId", correlationId);

        redisTemplate.opsForStream().add(
                MapRecord.create(STREAM_KEY, body),
                RedisStreamCommands.XAddOptions.maxlen(1000));
    }

    private void publishFallback(String payloadJson, String correlationId, Throwable t) {
        log.warn("Redis stream unavailable, live delivery skipped cid={}: {}",
                correlationId, t.getMessage());
    }
}