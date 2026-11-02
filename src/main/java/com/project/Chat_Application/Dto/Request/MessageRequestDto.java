package com.project.Chat_Application.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessageRequestDto {
    @NotNull
    private Long conversationId;
    @NotBlank
    private String content;

}
