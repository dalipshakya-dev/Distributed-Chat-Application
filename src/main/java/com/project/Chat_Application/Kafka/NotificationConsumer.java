package com.project.Chat_Application.Kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.Chat_Application.Dto.Response.NotificationResponseDto;
import com.project.Chat_Application.Events.KafkaTopics;
import com.project.Chat_Application.Events.MessageSentEvent;
import com.project.Chat_Application.Observability.CorrelationId;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final ConversationParticipantInterface conversationParticipantInterface;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationConsumer(ObjectMapper objectMapper,
                                ConversationParticipantInterface conversationParticipantInterface,
                                SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.conversationParticipantInterface = conversationParticipantInterface;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
            topics = KafkaTopics.MESSAGE_SENT,
            groupId = "notification-service"
    )
    public void handleMessageSent(String eventJson) throws Exception {
        MessageSentEvent event = objectMapper.readValue(eventJson, MessageSentEvent.class);
        MDC.put(CorrelationId.MDC_KEY, event.getCorrelationId() != null ? event.getCorrelationId() : "none");
        try {
            List<Long> participantIds = conversationParticipantInterface
                    .getParticipantUserIds(event.getConversationId());

            for (Long participantId : participantIds) {
                if (participantId.equals(event.getSenderId())) {
                    continue;
                }

                NotificationResponseDto dto = new NotificationResponseDto();
                dto.setConversationId(event.getConversationId());
                dto.setMessageId(event.getMessageId());
                dto.setSenderUsername(event.getSenderUsername());
                dto.setContent(event.getContent());
                dto.setCreatedAt(event.getCreatedAt());

                messagingTemplate.convertAndSend("/topic/notifications/" + participantId, dto);

                log.info("Notification sent to userId={} for conversationId={}",
                        participantId, event.getConversationId());
            }
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}