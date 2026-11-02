package com.project.Chat_Application.Dto.Response;

import com.project.Chat_Application.Entities.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessageResponseDto {
    private Long id;
    private Long conversationId;
    private UsersResponseDto sender;
    private String content;
    private MessageStatus status;
    private LocalDateTime createdAt;
}