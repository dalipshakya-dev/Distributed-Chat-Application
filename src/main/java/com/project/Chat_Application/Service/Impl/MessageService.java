package com.project.Chat_Application.Service.Impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.Chat_Application.Config.RateLimiterService;
import com.project.Chat_Application.Dto.DtoMapper.DtoMapper;
import com.project.Chat_Application.Dto.Request.MessageRequestDto;
import com.project.Chat_Application.Dto.Response.MessageResponseDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import com.project.Chat_Application.Entities.Conversation;
import com.project.Chat_Application.Entities.Message;
import com.project.Chat_Application.Entities.User;
import com.project.Chat_Application.Entities.enums.MessageStatus;
import com.project.Chat_Application.Events.KafkaTopics;
import com.project.Chat_Application.Events.MessageSentEvent;
import com.project.Chat_Application.Exceptions.RateLimitExceededException;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Observability.CorrelationId;
import com.project.Chat_Application.Repositories.ConversationRepository;
import com.project.Chat_Application.Repositories.MessageRepository;
import com.project.Chat_Application.Repositories.UsersRepository;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import com.project.Chat_Application.Service.Interfaces.MessageInterface;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MessageService implements MessageInterface {
    private final DtoMapper dtoMapper;
    private final MessageRepository messageRepository;
    private final UsersRepository usersRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantInterface conversationParticipantInterface;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RateLimiterService rateLimiterService;


    public MessageService(DtoMapper dtoMapper, MessageRepository messageRepository, UsersRepository usersRepository, ConversationRepository conversationRepository, ConversationParticipantInterface conversationParticipantInterface, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, RateLimiterService rateLimiterService) {
        this.dtoMapper = dtoMapper;
        this.messageRepository = messageRepository;
        this.usersRepository = usersRepository;
        this.conversationRepository = conversationRepository;
        this.conversationParticipantInterface = conversationParticipantInterface;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public MessageResponseDto createMessage(MessageRequestDto dto, Long senderId) {
        String rateLimitKey = "message:" + senderId;
        if (!rateLimiterService.isAllowed(rateLimitKey, 20, Duration.ofSeconds(10))) {
            log.warn("Rate limit exceeded for message creation by user {}", senderId);
            throw new RateLimitExceededException("You are sending messages too quickly. Please slow down.");
        }

        Message messageToSave = convertToEntity(dto);
        User sender = usersRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not Found!"));
        try {
            Conversation conversation = conversationRepository.findById(dto.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found!"));
            messageToSave.setConversation(conversation);
        } catch (ResourceNotFoundException r) {
            log.warn("failed attempt for sender's id to create message {}", senderId);
            throw r;
        }
        messageToSave.setStatus(MessageStatus.SENT);
        messageToSave.setSender(sender);
        messageToSave = messageRepository.save(messageToSave);
        log.info("Message Created Successfully by user {}", senderId);

        MessageResponseDto saved =  dtoMapper.convertMessageToDto(messageToSave);

        // publish event asynchronously to kafka!;
        try{
            MessageSentEvent event = MessageSentEvent.from(saved);
            event.setCorrelationId(CorrelationId.current());
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.MESSAGE_SENT,String.valueOf(dto.getConversationId()),eventJson);

            log.info("Published MessageSentEvent for messageId={}", saved.getId());

        }catch (Exception e){
            log.error("Failed to publish MessageSentEvent for messageId={}: {}",
                    saved.getId(), e.getMessage());
        }
        return saved;
    }

    @Override
    public MessageResponseDto getMessageById(Long id, Long currentUserId) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not Found!"));
        if (!conversationParticipantInterface.isParticipant(currentUserId, message.getConversation().getId())) {
            throw new AccessDeniedException("You can only get your own messages");
        }
        return dtoMapper.convertMessageToDto(message);
    }

    @Override
    public PagedResponseDto<MessageResponseDto> getMessagesByConversationId(Long conversationId, Pageable pageable, Long currentUserId) {
        if (!conversationParticipantInterface.isParticipant(currentUserId, conversationId)) {
            throw new AccessDeniedException("You can only get your own Conversations");
        }
        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageable);
        List<MessageResponseDto> content = messagePage.getContent().stream()
                .map(dtoMapper::convertMessageToDto)
                .toList();

        return new PagedResponseDto<>(
                content,
                messagePage.getNumber(),
                messagePage.getTotalPages(),
                messagePage.getTotalElements(),
                messagePage.isLast()
        );
    }

    @Override
    public boolean updateMessage(Long id, MessageRequestDto dto, Long currentUserId) {
        try {
            Message message = messageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Message Not Found!"));
            if (!message.getSender().getId().equals(currentUserId)) {
                log.warn("Unknown User is trying to edit messages {}", currentUserId);
                throw new AccessDeniedException("You can only edit your own messages");
            }
            message.setContent(dto.getContent());
            messageRepository.save(message);
            return true;
        } catch (ResourceNotFoundException r) {
            log.warn("Message not found by id {}", id);
            throw r;

        }
    }

    @Override
    public boolean deleteMessage(Long id, Long currentUserId) {
        try {
            Message message = messageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Message Not Found!"));
            if (!message.getSender().getId().equals(currentUserId)) {
                log.warn("Unknown user trying to delete the messages {}", id);
                throw new AccessDeniedException("You can only delete your own messages");
            }
            messageRepository.deleteById(id);
            return true;
        } catch (ResourceNotFoundException r) {
            log.warn("Messages not found by id {}", id);
            throw r;
        }
    }

    Message convertToEntity(MessageRequestDto dto) {
        Message message = new Message();
        message.setContent(dto.getContent());
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}