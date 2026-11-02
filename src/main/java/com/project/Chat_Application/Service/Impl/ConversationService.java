package com.project.Chat_Application.Service.Impl;

import com.project.Chat_Application.Dto.DtoMapper.DtoMapper;
import com.project.Chat_Application.Dto.Request.ConversationRequestDto;
import com.project.Chat_Application.Dto.Response.ConversationResponseDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import com.project.Chat_Application.Entities.Conversation;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Repositories.ConversationRepository;
import com.project.Chat_Application.Service.Interfaces.ConversationInterface;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService implements ConversationInterface {
    private final DtoMapper dtoMapper;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantInterface conversationParticipantInterface;

    public ConversationService(DtoMapper dtoMapper, ConversationRepository conversationRepository,
                               ConversationParticipantInterface conversationParticipantInterface) {
        this.dtoMapper = dtoMapper;
        this.conversationRepository = conversationRepository;
        this.conversationParticipantInterface = conversationParticipantInterface;
    }

    @Override
    public ConversationResponseDto createConversation(ConversationRequestDto conversation) {
        Conversation conversationToSave = convertToEntity(conversation);
        conversationToSave.setCreatedAt(LocalDateTime.now());
        Conversation savedConversation = conversationRepository.save(conversationToSave);
        return dtoMapper.convertConversationToDto(savedConversation);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponseDto getConversationId(Long id, Long currUserId) {
        if (!conversationParticipantInterface.isParticipant(currUserId,id)) {
            throw new AccessDeniedException("You can only view your own conversations");
        }
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found!"));

        return dtoMapper.convertConversationToDto(conversation);
    }

    @Override
    public boolean updateTypeById(Long id, ConversationRequestDto typeToUpdate, Long currUserId) {
        if (!conversationParticipantInterface.isParticipant(currUserId, id)) {
            throw new AccessDeniedException("You can only update your own conversations");
        }
        if (!conversationRepository.existsById(id)) {
            return false;
        }
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found!"));
        conversation.setType(typeToUpdate.getType());
        conversationRepository.save(conversation);
        return true;
    }

    @Override
    public boolean deleteConversation(Long id, Long currUserId) {
        if (!conversationParticipantInterface.isParticipant(currUserId, id)) {
            throw new AccessDeniedException("You can only delete your own conversations");
        }
        if (!conversationRepository.existsById(id)) {
            return false;
        }
        conversationRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<ConversationResponseDto> getAllConversation(Pageable page, Long currUserId) {
        List<Long> conversationIds = conversationParticipantInterface.getByUserId(currUserId, currUserId).stream()
                .map(cp -> cp.getConversation().getId())
                .toList();

        Page<Conversation> conversationPage = conversationRepository.findByIdIn(conversationIds, page);
        List<ConversationResponseDto> content = conversationPage.getContent().stream()
                .map(dtoMapper::convertConversationToDto)
                .toList();

        return new PagedResponseDto<>(
                content,
                conversationPage.getNumber(),
                conversationPage.getTotalPages(),
                conversationPage.getTotalElements(),
                conversationPage.isLast()
        );
    }

    Conversation convertToEntity(ConversationRequestDto dto) {
        Conversation conversation = new Conversation();
        conversation.setType(dto.getType());
        return conversation;
    }
}