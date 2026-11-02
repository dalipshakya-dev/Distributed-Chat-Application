package com.project.Chat_Application.Service.Interfaces;

import com.project.Chat_Application.Dto.Request.ConversationRequestDto;
import com.project.Chat_Application.Dto.Response.ConversationResponseDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import org.springframework.data.domain.Pageable;

public interface ConversationInterface {
    ConversationResponseDto createConversation(ConversationRequestDto conversation);
    ConversationResponseDto getConversationId(Long id,Long currUserId);
    boolean updateTypeById(Long id, ConversationRequestDto typeToUpdate,Long currUserId);
    boolean deleteConversation(Long id,Long currUserId);
    PagedResponseDto<ConversationResponseDto> getAllConversation(Pageable pageable,Long currUserId);

}
