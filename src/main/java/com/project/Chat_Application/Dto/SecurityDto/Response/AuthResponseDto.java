package com.project.Chat_Application.Dto.SecurityDto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuthResponseDto {
    private Long userId;
    private String username;
    private String token;
    private String tokenType;
    private String refreshToken;
}