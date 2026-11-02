package com.project.Chat_Application.Security;

import com.project.Chat_Application.Dto.Response.UsersResponseDto;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import com.project.Chat_Application.Service.Interfaces.UserInterface;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONVERSATION_TOPIC_PREFIX = "/topic/conversation/";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserInterface userInterface;
    private final ConversationParticipantInterface conversationParticipantInterface;
    private static final Logger log = LoggerFactory.getLogger(StompAuthInterceptor.class);

    public StompAuthInterceptor(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService,
                                UserInterface userInterface,
                                ConversationParticipantInterface conversationParticipantInterface) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userInterface = userInterface;
        this.conversationParticipantInterface = conversationParticipantInterface;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.info("invalid STOMP Message");
            throw new AccessDeniedException("Invalid STOMP message");
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnection(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            log.warn("Without authorization header");
            throw new AccessDeniedException("Authorization header is required");
        }
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Authorization header must use Bearer token");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (token.isBlank()) {
            log.warn("Jwt token is missing while auth connecting to stomp");
            throw new AccessDeniedException("JWT token is missing");
        }

        try {
            String username = jwtUtil.extractUsername(token);

            if (username == null || username.isBlank()) {
                log.warn("invalid jwt token!");
                throw new AccessDeniedException("Invalid JWT token");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtUtil.isTokenValid(token, userDetails)) {
                throw new AccessDeniedException("Invalid or expired JWT token");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);

        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessDeniedException("Invalid or expired JWT token", e);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();

        if (principal == null) {
            log.warn("UN-Authenticated User failed attempt!");
            throw new AccessDeniedException("User is not authenticated");
        }

        String destination = accessor.getDestination();

        if (destination == null || destination.isBlank()) {
            throw new AccessDeniedException("Subscription destination is required");
        }

        if (!destination.startsWith(CONVERSATION_TOPIC_PREFIX)) {
            return;
        }

        Long conversationId = extractConversationId(destination);
        UsersResponseDto user = userInterface.getByUsername(principal.getName());

        if (!conversationParticipantInterface.isParticipant(user.getId(), conversationId)) {
            log.warn("Failed to subscribe — user {} not a participant in conversation {}", user.getId(), conversationId);
            throw new AccessDeniedException("You are not allowed to subscribe to this conversation");
        }

    }

    private Long extractConversationId(String destination) {
        String conversationIdString = destination.substring(CONVERSATION_TOPIC_PREFIX.length());

        if (conversationIdString.isBlank() || !conversationIdString.matches("\\d+")) {
            throw new AccessDeniedException("Invalid conversation destination");
        }

        try {
            return Long.parseLong(conversationIdString);
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Invalid conversation ID", e);
        }
    }
}