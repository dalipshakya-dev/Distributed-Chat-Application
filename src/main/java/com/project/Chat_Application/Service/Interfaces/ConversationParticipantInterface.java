package com.project.Chat_Application.Service.Interfaces;
import com.project.Chat_Application.Dto.Response.ConversationParticipantResponseDto;
import com.project.Chat_Application.Dto.Response.UnReadCountResponseDto;


import java.util.List;

public interface ConversationParticipantInterface {
    ConversationParticipantResponseDto createParticipant(Long userId, Long conversationId);
    List<ConversationParticipantResponseDto> getByUserId(Long userId, Long currUserId);
    List<ConversationParticipantResponseDto> getByConversationId(Long conversationId,Long currUserId);
    ConversationParticipantResponseDto getByUserIdAndConversationId(Long userId, Long conversationId,Long currUserId);
    UnReadCountResponseDto getUnreadCount(Long conversationId, Long currentUserId);
    void markAsRead(Long conversationId,Long currentUserId);
    boolean leaveConversation(Long id,Long currUserId);
    boolean isParticipant(Long userId, Long conversationId);
    List<Long> getParticipantUserIds(Long conversationId);
    long getMySQLUnreadCount(Long conversationId,Long userId);

}
