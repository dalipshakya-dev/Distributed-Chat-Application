package com.project.Chat_Application.Kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.Chat_Application.Events.KafkaTopics;
import com.project.Chat_Application.Events.MessageSentEvent;
import com.project.Chat_Application.Observability.CorrelationId;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnreadCountConsumer {

    private static final Logger log = LoggerFactory.getLogger(UnreadCountConsumer.class);
    private static final String UNREAD_KEY_PREFIX = "unread:";

    private final ObjectMapper objectMapper;
    private final ConversationParticipantInterface conversationParticipantInterface;
    private final StringRedisTemplate stringRedisTemplate;

    public UnreadCountConsumer(ObjectMapper objectMapper,
                               ConversationParticipantInterface conversationParticipantInterface,
                               StringRedisTemplate stringRedisTemplate) {
        this.objectMapper = objectMapper;
        this.conversationParticipantInterface = conversationParticipantInterface;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @KafkaListener(
            topics = KafkaTopics.MESSAGE_SENT,
            groupId = "unread-count-service"
    )
    public void handleMessageSent(String eventJson) throws Exception {
        MessageSentEvent event = objectMapper.readValue(eventJson, MessageSentEvent.class);
        MDC.put(CorrelationId.MDC_KEY,
                event.getCorrelationId() != null ? event.getCorrelationId() : "none");
        try {
            List<Long> participantIds = conversationParticipantInterface
                    .getParticipantUserIds(event.getConversationId());

            for (Long participantId : participantIds) {
                if (participantId.equals(event.getSenderId())) {
                    continue;
                }

                String key = UNREAD_KEY_PREFIX + participantId + ":" + event.getConversationId();

                Boolean keyExists = stringRedisTemplate.hasKey(key);
                if (Boolean.FALSE.equals(keyExists)) {
                    long historicalCount = conversationParticipantInterface
                            .getMySQLUnreadCount(event.getConversationId(), participantId);
                    stringRedisTemplate.opsForValue().set(key, String.valueOf(historicalCount + 1));
                    log.info("Initialized unread count for userId={} in conversationId={} at {} (historical + 1)",
                            participantId, event.getConversationId(), historicalCount + 1);
                } else {
                    stringRedisTemplate.opsForValue().increment(key);
                    log.info("Incremented unread count for userId={} in conversationId={}",
                            participantId, event.getConversationId());
                }
            }
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}