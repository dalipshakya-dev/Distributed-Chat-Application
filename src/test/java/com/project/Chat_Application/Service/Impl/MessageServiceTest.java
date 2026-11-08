package com.project.Chat_Application.Service.Impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.Chat_Application.Config.RateLimiterService;
import com.project.Chat_Application.Dto.DtoMapper.DtoMapper;
import com.project.Chat_Application.Dto.Request.MessageRequestDto;
import com.project.Chat_Application.Dto.Response.MessageResponseDto;
import com.project.Chat_Application.Entities.Conversation;
import com.project.Chat_Application.Entities.Message;
import com.project.Chat_Application.Entities.User;
import com.project.Chat_Application.Entities.enums.ConversationType;
import com.project.Chat_Application.Entities.enums.MessageStatus;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Repositories.ConversationRepository;
import com.project.Chat_Application.Repositories.MessageRepository;
import com.project.Chat_Application.Repositories.UsersRepository;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;


import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantInterface conversationParticipantInterface;

    @Mock
    private DtoMapper dtoMapper;

    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private RateLimiterService rateLimiterService;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        messageService = new MessageService(dtoMapper, messageRepository, usersRepository,
                conversationRepository, conversationParticipantInterface,
                kafkaTemplate, objectMapper,rateLimiterService);
    }

    @Test
    void createMessage_returnsCorrectMessage(){
        User user = new User();
        user.setId(1L);
        user.setUsername("dalip");
        user.setEmail("dalip@gmail.com");

        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setType(ConversationType.PUBLIC);
        conversation.setCreatedAt(LocalDateTime.now());

        when(usersRepository.findById(1L)).thenReturn(Optional.of(user));
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(rateLimiterService.isAllowed(anyString(), anyInt(), any())).thenReturn(true);

        Message fakeSavedMessage = new Message();
        fakeSavedMessage.setId(1L);
        fakeSavedMessage.setSender(user);
        fakeSavedMessage.setConversation(conversation);
        fakeSavedMessage.setContent("Hello");
        fakeSavedMessage.setCreatedAt(LocalDateTime.now());

        when(messageRepository.save(any(Message.class))).thenReturn(fakeSavedMessage);

        MessageResponseDto fakeDto = new MessageResponseDto();
        fakeDto.setId(1L);
        fakeDto.setContent("Hello");
        when(dtoMapper.convertMessageToDto(any(Message.class))).thenReturn(fakeDto);

        MessageRequestDto requestDto = new MessageRequestDto();
        requestDto.setContent("Hello");
        requestDto.setConversationId(1L);

        MessageResponseDto result = messageService.createMessage(requestDto, 1L);

        assertEquals("Hello", result.getContent());
    }

    @Test
    void updateMessage_returnsTrueWhenOwner(){
        User sender = new User();
        sender.setId(1L);
        sender.setEmail("dalip@gmail.com");
        sender.setUsername("dalip");

        Message message = new Message();
        message.setId(1L);
        message.setStatus(MessageStatus.SENT);
        message.setContent("hello");
        message.setSender(sender);
        message.setCreatedAt(LocalDateTime.now());

        MessageRequestDto dto = new MessageRequestDto();
        dto.setConversationId(1L);
        dto.setContent("updated hello");

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        boolean result = messageService.updateMessage(1L, dto, 1L);

        assertTrue(result);
        assertEquals("updated hello", message.getContent());
    }

    @Test
    void updateMessage_throwsWhenNotOwner(){
        User sender = new User();
        sender.setId(1L);
        sender.setEmail("dalip@gmail.com");
        sender.setUsername("dalip");

        Message message = new Message();
        message.setId(1L);
        message.setStatus(MessageStatus.SENT);
        message.setContent("hello");
        message.setSender(sender);
        message.setCreatedAt(LocalDateTime.now());

        MessageRequestDto dto = new MessageRequestDto();
        dto.setConversationId(1L);
        dto.setContent("hello");

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThrows(AccessDeniedException.class, () -> {
            messageService.updateMessage(1L, dto, 2L);
        });
    }

    @Test
    void updateMessage_throwsWhenMessageNotFound(){
        MessageRequestDto dto = new MessageRequestDto();
        dto.setConversationId(1L);
        dto.setContent("hello");

        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            messageService.updateMessage(99L, dto, 1L);
        });
    }

    @Test
    void deleteMessage_returnsTrueWhenOwner(){
        User sender = new User();
        sender.setId(1L);
        sender.setEmail("dalip@gmail.com");
        sender.setUsername("dalip");

        Message message = new Message();
        message.setId(1L);
        message.setStatus(MessageStatus.SENT);
        message.setContent("hello");
        message.setSender(sender);
        message.setCreatedAt(LocalDateTime.now());

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        boolean result = messageService.deleteMessage(1L, 1L);

        assertTrue(result);
    }

    @Test
    void deleteMessage_throwsWhenNotOwner(){
        User sender = new User();
        sender.setId(1L);
        sender.setEmail("dalip@gmail.com");
        sender.setUsername("dalip");

        Message message = new Message();
        message.setId(1L);
        message.setStatus(MessageStatus.SENT);
        message.setContent("hello");
        message.setSender(sender);
        message.setCreatedAt(LocalDateTime.now());

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThrows(AccessDeniedException.class, () -> {
            messageService.deleteMessage(1L, 2L);
        });
    }

    @Test
    void deleteMessage_throwsWhenNotFound(){
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            messageService.deleteMessage(99L, 1L);
        });
    }
}