package com.project.Chat_Application.Controllers;

import com.project.Chat_Application.Dto.Request.ConversationRequestDto;
import com.project.Chat_Application.Dto.Response.ConversationResponseDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import com.project.Chat_Application.Security.CustomUserDetails;
import com.project.Chat_Application.Service.Interfaces.ConversationInterface;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationInterface conversationInterface;

    public ConversationController(ConversationInterface conversationInterface) {
        this.conversationInterface = conversationInterface;
    }

    @PostMapping
    public ResponseEntity<ConversationResponseDto> createConversation(@Valid @RequestBody ConversationRequestDto dto){
        ConversationResponseDto dto1 = conversationInterface.createConversation(dto);
        return new ResponseEntity<>(dto1, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> updateType(@PathVariable Long id, @Valid @RequestBody ConversationRequestDto type ,@AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        conversationInterface.updateTypeById(id, type,currUserId);
        return ResponseEntity.ok("Updated");
    }

    @GetMapping
    public ResponseEntity<PagedResponseDto<ConversationResponseDto>> getAllConversations(Pageable page,@AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        return ResponseEntity.ok(conversationInterface.getAllConversation(page,currUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponseDto> getConversationById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        return ResponseEntity.ok(conversationInterface.getConversationId(id,currUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteById(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        boolean deleted = conversationInterface.deleteConversation(id,currUserId);
        if(!deleted){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}