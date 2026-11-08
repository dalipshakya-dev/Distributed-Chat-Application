package com.project.Chat_Application.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    private String content;

}
