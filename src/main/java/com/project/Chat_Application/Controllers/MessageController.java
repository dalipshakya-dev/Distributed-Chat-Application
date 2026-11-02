package com.project.Chat_Application.Controllers;

import com.project.Chat_Application.Dto.Request.MessageRequestDto;
import com.project.Chat_Application.Dto.Response.MessageResponseDto;
import com.project.Chat_Application.Dto.Response.PagedResponseDto;
import com.project.Chat_Application.Security.CustomUserDetails;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import com.project.Chat_Application.Service.Interfaces.MessageInterface;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageInterface messageInterface;

    public MessageController(MessageInterface messageInterface, ConversationParticipantInterface conversationParticipantInterface) {
        this.messageInterface = messageInterface;
    }

    @PostMapping
    public ResponseEntity<MessageResponseDto> createMessage(@Valid @RequestBody MessageRequestDto dto,
                                                            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return new ResponseEntity<>(messageInterface.createMessage(dto, currentUser.getId()), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponseDto> getMessageById(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails currentUser) {
        Long currUserId = currentUser.getId();
        return ResponseEntity.ok(messageInterface.getMessageById(id,currUserId));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<PagedResponseDto<MessageResponseDto>> getMessagesByConversationId(@PathVariable Long conversationId, Pageable pageable,@AuthenticationPrincipal CustomUserDetails currentUser) {
        Long currUserId = currentUser.getId();
        return ResponseEntity.ok(messageInterface.getMessagesByConversationId(conversationId,pageable,currUserId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateMessage(@PathVariable Long id, @Valid @RequestBody MessageRequestDto dto,
                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        boolean updated = messageInterface.updateMessage(id, dto, currentUser.getId());
        if(!updated){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Message updated successfully!");
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails currentUser) {
        boolean deleted = messageInterface.deleteMessage(id, currentUser.getId());
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}