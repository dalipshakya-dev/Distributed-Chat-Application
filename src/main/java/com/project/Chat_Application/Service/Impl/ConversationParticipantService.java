package com.project.Chat_Application.Service.Impl;

import com.project.Chat_Application.Dto.DtoMapper.DtoMapper;
import com.project.Chat_Application.Dto.Response.ConversationParticipantResponseDto;
import com.project.Chat_Application.Dto.Response.UnReadCountResponseDto;
import com.project.Chat_Application.Entities.Conversation;
import com.project.Chat_Application.Entities.ConversationParticipant;
import com.project.Chat_Application.Entities.User;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Repositories.ConversationParticipantRepository;
import com.project.Chat_Application.Repositories.ConversationRepository;
import com.project.Chat_Application.Repositories.MessageRepository;
import com.project.Chat_Application.Repositories.UsersRepository;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cglib.core.Local;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationParticipantService implements ConversationParticipantInterface {

    private static final Logger log = LoggerFactory.getLogger(ConversationParticipantService.class);

    private final DtoMapper dtoMapper;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final UsersRepository usersRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public ConversationParticipantService(DtoMapper dtoMapper, ConversationParticipantRepository conversationParticipantRepository, UsersRepository usersRepository, ConversationRepository conversationRepository, MessageRepository messageRepository, CacheManager cacheManager, StringRedisTemplate stringRedisTemplate) {
        this.dtoMapper = dtoMapper;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.usersRepository = usersRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @CacheEvict(value = "participants", key = "#userId + ':' + #conversationId")
    public ConversationParticipantResponseDto createParticipant(Long userId, Long conversationId) {
        ConversationParticipant conversationParticipant = new ConversationParticipant();
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not Found!"));
            conversationParticipant.setConversation(conversation);
        } catch (ResourceNotFoundException e) {
            log.warn("Failed attempt to find conversation by id {}", conversationId);
            throw e;
        }
        try {
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
            conversationParticipant.setUser(user);
        } catch (ResourceNotFoundException r) {
            log.warn("Failed attempt to find user by id {}", userId);
            throw r;
        }
        conversationParticipant.setJoinedAt(LocalDateTime.now());
        conversationParticipant = conversationParticipantRepository.save(conversationParticipant);

        return convertToDto(conversationParticipant);
    }

    @Override
    public List<ConversationParticipantResponseDto> getByUserId(Long userId, Long currUserId) {
        if (!currUserId.equals(userId)) {
            throw new AccessDeniedException("You can only view your own participations");
        }
        List<ConversationParticipantResponseDto> dto = new ArrayList<>();
        for (ConversationParticipant cp : conversationParticipantRepository.findByUserId(userId)) {
            dto.add(convertToDto(cp));
        }
        return dto;
    }

    @Override
    public List<ConversationParticipantResponseDto> getByConversationId(Long conversationId, Long currUserId) {
        if (!isParticipant(currUserId, conversationId)) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }
        List<ConversationParticipantResponseDto> dto = new ArrayList<>();
        for (ConversationParticipant cp : conversationParticipantRepository.findByConversationId(conversationId)) {
            dto.add(convertToDto(cp));
        }
        return dto;
    }

    @Override
    public ConversationParticipantResponseDto getByUserIdAndConversationId(Long userId, Long conversationId, Long currUserId) {
        if (!currUserId.equals(userId)) {
            throw new AccessDeniedException("You can only view your own participation");
        }
        ConversationParticipant participant = conversationParticipantRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a participant"));
        return convertToDto(participant);
    }

    @Override
    public UnReadCountResponseDto getUnreadCount(Long conversationId, Long currentUserId) {
        // check Redis counter first
        String key = "unread:" + currentUserId + ":" + conversationId;
        Object cached = stringRedisTemplate.opsForValue().get(key);

        long unreadCount;
        if (cached != null) {
            unreadCount = Long.parseLong(cached.toString());
            log.info("Unread count from Redis for userId={} conversationId={}: {}",
                    currentUserId, conversationId, unreadCount);
        } else {
            // fallback to MySQL if no Redis counter exists yet
            ConversationParticipant conversationParticipant = conversationParticipantRepository
                    .findByUserIdAndConversationId(currentUserId, conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation or user not found!"));

            LocalDateTime lastReadAt = conversationParticipant.getLastReadAt();
            unreadCount = (lastReadAt == null)
                    ? messageRepository.countByConversationId(conversationId)
                    : messageRepository.countByConversationIdAndCreatedAtAfter(conversationId, lastReadAt);

            log.info("Unread count from MySQL for userId={} conversationId={}: {}",
                    currentUserId, conversationId, unreadCount);
        }

        UnReadCountResponseDto dto = new UnReadCountResponseDto();
        dto.setConversationId(conversationId);
        dto.setUnreadCount(unreadCount);
        return dto;
    }

    @Override
    public void markAsRead(Long conversationId, Long currentUserId) {
        if (!isParticipant(currentUserId, conversationId)) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }
        ConversationParticipant cp = conversationParticipantRepository
                .findByUserIdAndConversationId(currentUserId, conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation or user not found!"));
        cp.setLastReadAt(LocalDateTime.now());
        conversationParticipantRepository.save(cp);

        // reset Redis unread counter
        String key = "unread:" + currentUserId + ":" + conversationId;
        stringRedisTemplate.delete(key);
        log.info("Reset unread count for userId={} in conversationId={}", currentUserId, conversationId);
    }

    @Override
    @CacheEvict(value =  "participants",key = "#userId + ':' + #conversationId")
    public boolean leaveConversation(Long id, Long currUserId) {
        ConversationParticipant participant = conversationParticipantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found!"));

        if (!participant.getUser().getId().equals(currUserId)) {
            throw new AccessDeniedException("You can only remove your own participation");
        }

        conversationParticipantRepository.deleteById(id);

        return true;
    }

    @Override
    @Cacheable(value = "participants", key = "#userId + ':' + #conversationId")
    public boolean isParticipant(Long userId, Long conversationId) {
        return conversationParticipantRepository
                .findByUserIdAndConversationId(userId, conversationId).isPresent();
    }


    @Override
    public List<Long> getParticipantUserIds(Long conversationId) {
        return conversationParticipantRepository.findByConversationId(conversationId)
                .stream()
                .map(cp ->cp.getUser().getId())
                .toList();
    }

    @Override
    public long getMySQLUnreadCount(Long conversationId, Long userId) {
        ConversationParticipant cp = conversationParticipantRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElse(null);
        if (cp == null) return 0;
        LocalDateTime lastReadAt = cp.getLastReadAt();
        return (lastReadAt == null)
                ? messageRepository.countByConversationId(conversationId)
                : messageRepository.countByConversationIdAndCreatedAtAfter(conversationId, lastReadAt);
    }

    ConversationParticipantResponseDto convertToDto(ConversationParticipant cp) {
        ConversationParticipantResponseDto dto = new ConversationParticipantResponseDto();
        dto.setId(cp.getId());
        dto.setJoinedAt(cp.getJoinedAt());
        dto.setLastReadAt(cp.getLastReadAt());
        dto.setUser(dtoMapper.convertUserToDto(cp.getUser()));
        dto.setConversation(dtoMapper.convertConversationSummaryToDto(cp.getConversation()));
        return dto;
    }
}