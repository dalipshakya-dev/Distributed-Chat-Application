package com.project.Chat_Application.Service.Impl;

import com.project.Chat_Application.Dto.DtoMapper.DtoMapper;
import com.project.Chat_Application.Dto.Request.UsersRequestDto;
import com.project.Chat_Application.Dto.Response.UsersResponseDto;
import com.project.Chat_Application.Entities.User;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;


class UserServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private DtoMapper dtoMapper;

    private UserService userService;
    @Mock
    private  CacheManager cacheManager;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(usersRepository, dtoMapper,cacheManager);
    }

    @Test
    void getUserById_returnsCorrectUser(){
        User fakeUser = new User();
        fakeUser.setId(1L);
        fakeUser.setUsername("neha");
        fakeUser.setEmail("neha@example.com");

        when(usersRepository.findById(1L)).thenReturn(Optional.of(fakeUser));

        UsersResponseDto fakeDto = new UsersResponseDto(1L, "neha", "neha@example.com");
        when(dtoMapper.convertUserToDto(fakeUser)).thenReturn(fakeDto);

        UsersResponseDto result = userService.getUserById(1L);

        assertEquals("neha", result.getUsername());
    }

    @Test
    void getUserById_throwsWhenNotFound(){
        when(usersRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(99L);
        });
    }

    @Test
    void createUser(){
        User fakeSavedUser = new User();
        fakeSavedUser.setId(1L);
        fakeSavedUser.setUsername("neha");
        fakeSavedUser.setEmail("neha@example.com");

        when(usersRepository.save(any(User.class))).thenReturn(fakeSavedUser);
        UsersResponseDto dto = new UsersResponseDto(1L,"neha","neha@gmail.com");
        when(dtoMapper.convertUserToDto(fakeSavedUser)).thenReturn(dto);

        UsersRequestDto requestDto = new UsersRequestDto();
        requestDto.setUsername("neha");
        requestDto.setEmail("neha@example.com");

        UsersResponseDto result = userService.createUser(requestDto);
        assertEquals("neha",result.getUsername());

    }



    @Test
    void updateUser_returnsTrueWhenSuccessful(){
        User fakeExistingUser = new User();
        fakeExistingUser.setId(1L);
        fakeExistingUser.setUsername("oldName");
        fakeExistingUser.setEmail("old@example.com");

        when(usersRepository.findById(1L)).thenReturn(Optional.of(fakeExistingUser));

        UsersRequestDto updateDto = new UsersRequestDto();
        updateDto.setUsername("Dalip");
        updateDto.setEmail("dalip@gmail.com");

        boolean result = userService.updateUser(1L,updateDto,1L);
        assertTrue(result);
        assertEquals("Dalip",fakeExistingUser.getUsername());

    }


    @Test
    void deleteUser_returnsTrueWhenSuccessful(){
        User user = new User();
        user.setId(1L);
        user.setUsername("Dalip");
        user.setEmail("dalipshakya@gmail.com");

        when(usersRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean result = userService.deleteUser(1L,1L);
        assertTrue(result);
    }

    @Test
    void deleteUser_throwsResourceNotFound_whenUserDoesNotExist(){
        when(usersRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUser(99L, 99L));
    }





}