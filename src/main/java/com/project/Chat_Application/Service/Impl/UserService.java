package com.project.Chat_Application.Service.Impl;

import com.project.Chat_Application.Dto.DtoMapper.DtoMapper;
import com.project.Chat_Application.Dto.Request.UsersRequestDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import com.project.Chat_Application.Dto.Response.UsersResponseDto;
import com.project.Chat_Application.Entities.User;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Repositories.UsersRepository;
import com.project.Chat_Application.Service.Interfaces.UserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserInterface {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final DtoMapper dtoMapper;
    private final UsersRepository usersRepository;
    private final CacheManager cacheManager;

    public UserService(UsersRepository usersRepository, DtoMapper dtoMapper,
                       CacheManager cacheManager) {
        this.usersRepository = usersRepository;
        this.dtoMapper = dtoMapper;
        this.cacheManager = cacheManager;
    }

    @Override
    public UsersResponseDto createUser(UsersRequestDto user) {
        User savedUser = usersRepository.save(convertToEntity(user));
        return dtoMapper.convertUserToDto(savedUser);
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UsersResponseDto getUserById(Long id) {
        User user = usersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        return dtoMapper.convertUserToDto(user);
    }

    @Override
    public PagedResponseDto<UsersResponseDto> getAllUsers(Pageable page) {
        Page<User> users = usersRepository.findAll(page);
        List<UsersResponseDto> content = users.getContent().stream()
                .map(dtoMapper::convertUserToDto)
                .toList();
        return new PagedResponseDto<>(
                content,
                users.getNumber(),
                users.getTotalPages(),
                users.getTotalElements(),
                users.isLast()
        );
    }

    @Override
    @CacheEvict(value = "users", key = "#id")
    public boolean updateUser(Long id, UsersRequestDto updateUser, Long currUserId) {
        if (!currUserId.equals(id)) {
            throw new AccessDeniedException("You can only update your own account");
        }
        try {
            User exists = usersRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
            // evict old username from cache before changing it
            evictUsernameCache(exists.getUsername());
            exists.setEmail(updateUser.getEmail());
            exists.setUsername(updateUser.getUsername());
            usersRepository.save(exists);
            return true;
        } catch (ResourceNotFoundException r) {
            log.warn("Failed attempt to update user by id {} {}", id, updateUser.getUsername());
            throw r;
        }
    }

    @Override
    @CacheEvict(value = "users", key = "#id")
    public boolean deleteUser(Long id, Long currUserId) {
        if (!currUserId.equals(id)) {
            throw new AccessDeniedException("You can only delete your own account");
        }
        try {
            User user = usersRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
            // evict username cache before deleting
            evictUsernameCache(user.getUsername());
            usersRepository.deleteById(id);
            return true;
        } catch (ResourceNotFoundException r) {
            log.warn("Failed attempt to delete user by id {}", id);
            return false;
        }
    }

    @Override
    @Cacheable(value = "usersByUsername", key = "#userName")
    public UsersResponseDto getByUsername(String userName) {
        User user = usersRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        return dtoMapper.convertUserToDto(user);
    }

    private void evictUsernameCache(String username) {
        var cache = cacheManager.getCache("usersByUsername");
        if (cache != null) {
            cache.evict(username);
        }
    }

    User convertToEntity(UsersRequestDto dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        return user;
    }
}