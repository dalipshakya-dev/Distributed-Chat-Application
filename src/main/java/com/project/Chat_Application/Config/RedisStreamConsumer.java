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
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RedisStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamConsumer.class);
    private static final String STREAM_KEY = "chat-messages";
    private static final String DEAD_LETTER_KEY = "chat-messages.DLT";
    private static final String LAST_ID_KEY_PREFIX = "stream:lastId:";
    private static final String START_OF_STREAM = "0-0";
    private static final long BATCH_SIZE = 100;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final String instanceId;

    private volatile String lastId;

    public RedisStreamConsumer(
            @Qualifier("customRedisTemplate") @Lazy RedisTemplate<String, Object> redisTemplate,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.instanceId = resolveInstanceId();
    }

    private String resolveInstanceId() {
        String fromEnv = System.getenv("INSTANCE_ID");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;
        String host = System.getenv("HOSTNAME");
        if (host != null && !host.isBlank()) return host;
        String fallback = UUID.randomUUID().toString();
        log.warn("No INSTANCE_ID or HOSTNAME set, using random id {}. Checkpoints will not survive restarts.", fallback);
        return fallback;
    }

    @PostConstruct
    public void init() {
        loadPosition();
    }

    private void loadPosition() {
        try {
            String saved = stringRedisTemplate.opsForValue().get(LAST_ID_KEY_PREFIX + instanceId);
            if (saved != null && !saved.isBlank()) {
                lastId = saved;
                log.info("Stream consumer resumed at position {} for instance {}", lastId, instanceId);
            } else {
                lastId = START_OF_STREAM;
                log.info("No saved position, starting from beginning, instance={}", instanceId);
            }
        } catch (Exception e) {
            log.warn("Checkpoint unreadable, will retry on next poll: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 500)
    public void pollStream() {
        if (lastId == null) {
            loadPosition();
            if (lastId == null) return;
        }

        try {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(StreamReadOptions.empty().count(BATCH_SIZE),
                            StreamOffset.create(STREAM_KEY, ReadOffset.from(lastId)));

            if (records == null || records.isEmpty()) {
                return;
            }

            log.debug("Redis consumer received {} record(s), instance={}", records.size(), instanceId);

            for (MapRecord<String, Object, Object> record : records) {
                processRecord(record);
            }
        } catch (Exception e) {
            log.error("Failed to poll Redis stream: {}", e.getMessage());
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        String recordId = record.getId().getValue();
        Object cid = record.getValue().get("correlationId");
        MDC.put(CorrelationId.MDC_KEY, cid != null ? cid.toString() : "none");
        try {
            deliver(record);
        } catch (Exception e) {
            log.error("Could not process stream record {}, sending to dead letter: {}", recordId, e.getMessage());
            deadLetter(record, e);
        } finally {
            advance(recordId);
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    private void deliver(MapRecord<String, Object, Object> record) throws Exception {
        Object payload = record.getValue().get("payload");
        if (payload == null) {
            throw new IllegalArgumentException("record has no payload");
        }
        MessageResponseDto dto = objectMapper.readValue(payload.toString(), MessageResponseDto.class);
        messagingTemplate.convertAndSend("/topic/conversation/" + dto.getConversationId(), dto);
        log.info("Redis delivered message {} to /topic/conversation/{}", dto.getId(), dto.getConversationId());
    }

    private void deadLetter(MapRecord<String, Object, Object> record, Exception cause) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("originalId", record.getId().getValue());
            body.put("error", String.valueOf(cause.getMessage()));
            body.put("payload", String.valueOf(record.getValue().get("payload")));
            redisTemplate.opsForStream().add(MapRecord.create(DEAD_LETTER_KEY, body));
        } catch (Exception e) {
            log.error("Dead-letter write failed for record {}: {}", record.getId().getValue(), e.getMessage());
        }
    }

    private void advance(String recordId) {
        lastId = recordId;
        try {
            stringRedisTemplate.opsForValue().set(LAST_ID_KEY_PREFIX + instanceId, recordId);
        } catch (Exception e) {
            log.warn("Checkpoint save failed at {}, a restart may replay from an earlier position: {}",
                    recordId, e.getMessage());
        }
    }
}