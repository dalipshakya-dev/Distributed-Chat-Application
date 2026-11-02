package com.project.Chat_Application.Dto.SecurityDto.Request;



import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginRequestDto {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}