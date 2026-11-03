package com.project.Chat_Application.Service.Impl;

import com.project.Chat_Application.Dto.DtoMapper.DtoMapper;
import com.project.Chat_Application.Dto.Request.ConversationRequestDto;
import com.project.Chat_Application.Dto.Response.ConversationResponseDto;
import com.project.Chat_Application.Entities.Conversation;
import com.project.Chat_Application.Entities.enums.ConversationType;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Repositories.ConversationRepository;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private DtoMapper dtoMapper;
    @Mock
    private ConversationParticipantInterface conversationParticipantInterface;

    @InjectMocks
    private ConversationService conversationService;


    @Test
    void getConversationId_throwsAccessDenied_whenNotParticipant(){
        //given
        Long id = 1L;
        Long userId = 10L;
        when(conversationParticipantInterface.isParticipant(userId,id)).thenReturn(false);

        //when / then
        assertThrows(AccessDeniedException.class,()->conversationService.getConversationId(id,userId));

    }

    @Test
    void getConversationId_throwsResourceNotFound_whenConversationMissing(){
        Long conversationId = 1L;
        Long userId = 10L;
        when(conversationParticipantInterface.isParticipant(userId,conversationId)).thenReturn(true);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->conversationService.getConversationId(conversationId,userId));
    }

    @Test
    void getConversationId_returnsDto_whenParticipantAndConversationExists(){
        //given
        Long conversationId = 1L;
        Long userId = 10L;

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        ConversationResponseDto expectedDto = new ConversationResponseDto();
        expectedDto.setId(conversationId);

        when(conversationParticipantInterface.isParticipant(userId,conversationId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        when(dtoMapper.convertConversationToDto(conversation)).thenReturn(expectedDto);

        ConversationResponseDto result = conversationService.getConversationId(conversationId,userId);

        assertEquals(expectedDto,result);
        assertEquals(conversationId,result.getId());
    }



    @Test
    void updateTypeById_throwsAccessDenied_whenNotParticipant(){
        Long conversationId = 1L;
        Long userId = 10L;
        when(conversationParticipantInterface.isParticipant(userId, conversationId)).thenReturn(false);

        ConversationRequestDto dto = new ConversationRequestDto();
        //when / then
        assertThrows(AccessDeniedException.class,()->conversationService.updateTypeById(conversationId,dto,userId));
    }

    @Test
    void updateTypeById_returnsFalse_whenConversationDoesNotExist(){
        Long conversationId = 1L;
        Long userId = 10L;
        when(conversationParticipantInterface.isParticipant(userId, conversationId)).thenReturn(true);
        when(conversationRepository.existsById(conversationId)).thenReturn(false);

        ConversationRequestDto dto = new ConversationRequestDto();

        boolean result = conversationService.updateTypeById(conversationId, dto, userId);

        assertFalse(result);
    }

    @Test
    void updateTypeById_updatesTypeAndReturnsTrue_whenAuthorizedAndExists() {
        // given
        Long conversationId = 1L;
        Long userId = 10L;

        Conversation existingConversation = new Conversation();
        existingConversation.setId(conversationId);
        existingConversation.setType(ConversationType.PRIVATE);

        ConversationRequestDto dto = new ConversationRequestDto();
        dto.setType(ConversationType.PUBLIC);

        when(conversationParticipantInterface.isParticipant(userId, conversationId))
                .thenReturn(true);
        when(conversationRepository.existsById(conversationId))
                .thenReturn(true);
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(existingConversation));

        // when
        boolean result = conversationService.updateTypeById(conversationId, dto, userId);

        // then
        assertTrue(result);
        assertEquals(ConversationType.PUBLIC, existingConversation.getType());
        verify(conversationRepository).save(existingConversation);
    }

    @Test
    void deleteConversation_throwsAccessDenied_whenNotParticipant(){
        Long conversationId = 1L;
        Long userId = 10L;
        when(conversationParticipantInterface.isParticipant(userId, conversationId)).thenReturn(false);

        //when / then
        assertThrows(AccessDeniedException.class,()->conversationService.deleteConversation(conversationId,userId));
    }

    @Test
    void deleteConversation_returnsFalse_whenConversationDoesNotExist(){
        Long conversationId = 1L;
        Long userId = 10L;
        when(conversationParticipantInterface.isParticipant(userId, conversationId)).thenReturn(true);
        when(conversationRepository.existsById(conversationId)).thenReturn(false);

        boolean result = conversationService.deleteConversation(conversationId, userId);

        assertFalse(result);
    }


    @Test
    void deleteConversation_deletesAndReturnsTrue_whenAuthorizedAndExists(){
        Long conversationId = 1L;
        Long userId = 10L;

        when(conversationParticipantInterface.isParticipant(userId, conversationId))
                .thenReturn(true);
        when(conversationRepository.existsById(conversationId))
                .thenReturn(true);

        boolean result = conversationService.deleteConversation(conversationId,userId);
        assertTrue(result);
        verify(conversationRepository).deleteById(conversationId);
    }
}