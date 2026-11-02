package com.project.Chat_Application.Events;

import com.project.Chat_Application.Dto.Response.MessageResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageSentEvent {
    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private String createdAt;
    private String correlationId;

    public static MessageSentEvent from(MessageResponseDto dto) {
        MessageSentEvent event = new MessageSentEvent();
        event.setMessageId(dto.getId());
        event.setConversationId(dto.getConversationId());
        event.setSenderId(dto.getSender().getId());
        event.setSenderUsername(dto.getSender().getUsername());
        event.setContent(dto.getContent());
        event.setCreatedAt(dto.getCreatedAt().toString());

        return event;
    }
}