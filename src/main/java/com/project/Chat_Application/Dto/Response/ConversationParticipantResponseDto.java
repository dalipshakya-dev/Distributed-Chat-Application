package com.project.Chat_Application.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ConversationParticipantResponseDto {
    private Long id;
    private UsersResponseDto user;
    private ConversationSummaryDto conversation;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
}