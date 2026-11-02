package com.project.Chat_Application.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UsersResponseDto {
    private Long id;
    private String username;
    private String email;
}
