package com.project.Chat_Application.Dto.Request;

import com.project.Chat_Application.Entities.enums.ConversationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ConversationRequestDto {
    @NotNull
    private ConversationType type;
}
