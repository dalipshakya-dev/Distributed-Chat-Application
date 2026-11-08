package com.project.Chat_Application.Dto.Request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UsersRequestDto {
    @NotBlank
    @Column(unique = true)
    private String username;

    @NotBlank
    @Email
    private String email;
}
