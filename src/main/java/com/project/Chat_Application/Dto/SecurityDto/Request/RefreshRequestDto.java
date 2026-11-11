package com.project.Chat_Application.Dto.SecurityDto.Request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RefreshRequestDto {
    @NotBlank
    private String refreshToken;
}