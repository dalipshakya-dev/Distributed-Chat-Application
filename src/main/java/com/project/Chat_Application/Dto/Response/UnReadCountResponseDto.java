package com.project.Chat_Application.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnReadCountResponseDto {
    private Long conversationId;
    private long unreadCount;
    private LocalDateTime lastReadAt;
}
