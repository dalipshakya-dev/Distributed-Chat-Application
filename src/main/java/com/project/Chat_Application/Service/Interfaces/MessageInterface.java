package com.project.Chat_Application.Service.Interfaces;

import com.project.Chat_Application.Dto.Request.MessageRequestDto;
import com.project.Chat_Application.Dto.Response.MessageResponseDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import org.springframework.data.domain.Pageable;

public interface MessageInterface {

    MessageResponseDto createMessage(MessageRequestDto dto,Long senderId);
    MessageResponseDto getMessageById(Long id, Long currUserId);
    PagedResponseDto<MessageResponseDto> getMessagesByConversationId(Long conversationId, Pageable pageable,Long currUserId);
    boolean updateMessage(Long id, MessageRequestDto dto,Long currentUserId);
    boolean deleteMessage(Long id,Long currentUserId);

}
