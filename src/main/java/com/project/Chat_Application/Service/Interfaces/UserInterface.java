package com.project.Chat_Application.Service.Interfaces;

import com.project.Chat_Application.Dto.Request.UsersRequestDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import com.project.Chat_Application.Dto.Response.UsersResponseDto;
import org.springframework.data.domain.Pageable;

public interface UserInterface {
    UsersResponseDto createUser(UsersRequestDto user);
    UsersResponseDto getUserById(Long id);
    PagedResponseDto<UsersResponseDto> getAllUsers(Pageable page);
    boolean updateUser(Long id ,UsersRequestDto updateUser,Long currUserId);
    boolean deleteUser(Long id,Long currUserId);
    UsersResponseDto getByUsername(String userName);

}
