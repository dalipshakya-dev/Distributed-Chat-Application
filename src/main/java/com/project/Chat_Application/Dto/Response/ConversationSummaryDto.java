package com.project.Chat_Application.Dto.Response;

import com.project.Chat_Application.Entities.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ConversationSummaryDto {
    private Long id;
    private ConversationType type;
}