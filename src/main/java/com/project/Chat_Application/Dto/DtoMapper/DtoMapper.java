package com.project.Chat_Application.Dto.DtoMapper;

import com.project.Chat_Application.Dto.Response.ConversationResponseDto;
import com.project.Chat_Application.Dto.Response.ConversationSummaryDto;
import com.project.Chat_Application.Dto.Response.MessageResponseDto;
import com.project.Chat_Application.Dto.Response.UsersResponseDto;
import com.project.Chat_Application.Entities.Conversation;
import com.project.Chat_Application.Entities.Message;
import com.project.Chat_Application.Entities.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DtoMapper {

    public UsersResponseDto convertUserToDto(User user) {
        UsersResponseDto responseDto = new UsersResponseDto();
        responseDto.setId(user.getId());
        responseDto.setEmail(user.getEmail());
        responseDto.setUsername(user.getUsername());
        return responseDto;
    }

    public MessageResponseDto convertMessageToDto(Message message) {
        MessageResponseDto dto = new MessageResponseDto();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setStatus(message.getStatus());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setConversationId(message.getConversation().getId());
        dto.setSender(convertUserToDto(message.getSender()));
        return dto;
    }

    public ConversationSummaryDto convertConversationSummaryToDto(Conversation conversation) {
        ConversationSummaryDto dto = new ConversationSummaryDto();
        dto.setId(conversation.getId());
        dto.setType(conversation.getType());
        return dto;
    }

    public ConversationResponseDto convertConversationToDto(Conversation conversation){
        ConversationResponseDto dto = new ConversationResponseDto();
        dto.setId(conversation.getId());
        dto.setType(conversation.getType());
        dto.setCreatedAt(conversation.getCreatedAt());

        List<MessageResponseDto> messageDtos = new ArrayList<>();
        for (Message message : conversation.getMessages()) {
            messageDtos.add(convertMessageToDto(message));  // reuse Job B here
        }
        dto.setMessages(messageDtos);

        return dto;

    }
}