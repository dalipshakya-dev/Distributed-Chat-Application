package com.project.Chat_Application.Dto.Response;



import com.project.Chat_Application.Entities.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ConversationResponseDto {
    private Long id;
    private ConversationType type;
    private LocalDateTime createdAt;
    private List<MessageResponseDto> messages;
}