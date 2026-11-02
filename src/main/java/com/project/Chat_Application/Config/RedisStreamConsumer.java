package com.project.Chat_Application.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.Chat_Application.Dto.Response.MessageResponseDto;
import com.project.Chat_Application.Observability.CorrelationId;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamConsumer.class);
    private static final String STREAM_KEY = "chat-messages";
    private static final String LAST_ID_KEY_PREFIX = "stream:lastId:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final String instanceId;

    private volatile String lastId = "0-0";

    public RedisStreamConsumer(
            @Qualifier("customRedisTemplate") @Lazy RedisTemplate<String, Object> redisTemplate,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.instanceId = System.getenv("INSTANCE_ID") != null
                ? System.getenv("INSTANCE_ID")
                : (System.getenv("HOSTNAME") != null
                ? System.getenv("HOSTNAME")
                : java.util.UUID.randomUUID().toString());
    }

    @PostConstruct
    public void init() {
        try {
            String savedPosition = stringRedisTemplate.opsForValue()
                    .get(LAST_ID_KEY_PREFIX + instanceId);
            if (savedPosition != null && !savedPosition.isBlank()) {
                lastId = savedPosition;
                log.info("Stream consumer resumed at position {} for instance {}", lastId, instanceId);
            } else {
                lastId = "0-0";
                log.info("No saved position — starting from beginning, instance={}", instanceId);
            }
        } catch (Exception e) {
            lastId = "0-0";
            log.warn("Could not read saved position, starting from beginning: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 500)
    public void pollStream() {
        try {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(StreamOffset.create(STREAM_KEY, ReadOffset.from(lastId)));

            if (records == null || records.isEmpty()) {
                return;
            }

            log.debug("Redis consumer received {} record(s), instance={}", records.size(), instanceId);

            for (MapRecord<String, Object, Object> record : records) {
                Object cid = record.getValue().get("correlationId");
                MDC.put(CorrelationId.MDC_KEY, cid != null ? cid.toString() : "none");
                try {
                    Object payload = record.getValue().get("payload");
                    if (payload != null) {
                        MessageResponseDto dto = objectMapper.readValue(payload.toString(), MessageResponseDto.class);
                        messagingTemplate.convertAndSend("/topic/conversation/" + dto.getConversationId(), dto);
                        log.info("Redis delivered message {} to /topic/conversation/{}",
                                dto.getId(), dto.getConversationId());
                    }
                    lastId = record.getId().getValue();
                    stringRedisTemplate.opsForValue().set(LAST_ID_KEY_PREFIX + instanceId, lastId);
                } finally {
                    MDC.remove(CorrelationId.MDC_KEY);
                }
            }
        } catch (Exception e) {
            log.error("Failed to poll Redis stream: {}", e.getMessage());
        }
    }
}