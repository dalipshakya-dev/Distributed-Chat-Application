package com.project.Chat_Application.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponseDto {
    private Long conversationId;
    private Long messageId;
    private String senderUsername;
    private String content;
    private String createdAt;
}