package com.project.Chat_Application.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.Chat_Application.Config.ChatStreamPublisher;
import com.project.Chat_Application.Dto.Request.MessageRequestDto;
import com.project.Chat_Application.Dto.Response.MessageResponseDto;
import com.project.Chat_Application.Dto.Response.UsersResponseDto;
import com.project.Chat_Application.Exceptions.RateLimitExceededException;
import com.project.Chat_Application.Observability.CorrelationId;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import com.project.Chat_Application.Service.Interfaces.MessageInterface;
import com.project.Chat_Application.Service.Interfaces.UserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageInterface messageInterface;
    private final UserInterface userInterface;
    private final ConversationParticipantInterface conversationParticipantInterface;
    private final ObjectMapper objectMapper;
    private final ChatStreamPublisher chatStreamPublisher;

    public ChatWebSocketController(
            MessageInterface messageInterface,
            UserInterface userInterface,
            ConversationParticipantInterface conversationParticipantInterface,
            ObjectMapper objectMapper,
            ChatStreamPublisher chatStreamPublisher) {
        this.messageInterface = messageInterface;
        this.userInterface = userInterface;
        this.conversationParticipantInterface = conversationParticipantInterface;
        this.objectMapper = objectMapper;
        this.chatStreamPublisher = chatStreamPublisher;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageRequestDto dto, Principal principal)
            throws JsonProcessingException {
        if (principal == null) {
            log.warn("Unauthenticated user trying to send a message over STOMP");
            throw new AccessDeniedException("Not authenticated");
        }

        UsersResponseDto sender = userInterface.getByUsername(principal.getName());

        if (!conversationParticipantInterface.isParticipant(sender.getId(), dto.getConversationId())) {
            log.warn("Failed attempt — user {} not a participant in conversation {}",
                    sender.getId(), dto.getConversationId());
            throw new AccessDeniedException("You are not allowed to send message!");
        }

        MessageResponseDto saved = messageInterface.createMessage(dto, sender.getId());
        log.info("Message Successfully Send to {}", sender.getId());

        String jsonString = objectMapper.writeValueAsString(saved);
        chatStreamPublisher.publish(jsonString, CorrelationId.orNone());
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser("/queue/errors")
    public String handleAccessDenied(AccessDeniedException ex) {
        log.warn("STOMP access denied: {}", ex.getMessage());
        return ex.getMessage();
    }

    @MessageExceptionHandler(RateLimitExceededException.class)
    @SendToUser("/queue/errors")
    public String handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("STOMP rate limit exceeded: {}", ex.getMessage());
        return ex.getMessage();
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public String handleGeneral(Exception ex) {
        log.error("Unhandled STOMP exception", ex);
        return "Something went wrong. Please try again.";
    }
}