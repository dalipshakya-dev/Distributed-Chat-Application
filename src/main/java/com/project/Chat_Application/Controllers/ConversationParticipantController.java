package com.project.Chat_Application.Controllers;

import com.project.Chat_Application.Dto.Response.ConversationParticipantResponseDto;
import com.project.Chat_Application.Dto.Response.UnReadCountResponseDto;
import com.project.Chat_Application.Security.CustomUserDetails;
import com.project.Chat_Application.Service.Interfaces.ConversationParticipantInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participants")
public class ConversationParticipantController {
    private final ConversationParticipantInterface conversationParticipantInterface;


    public ConversationParticipantController(ConversationParticipantInterface conversationParticipantInterface) {
        this.conversationParticipantInterface = conversationParticipantInterface;
    }

    @PostMapping
    public ResponseEntity<ConversationParticipantResponseDto> createParticipant(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestParam Long conversationId){
        return new ResponseEntity<>(conversationParticipantInterface.createParticipant(customUserDetails.getId(), conversationId), HttpStatus.CREATED);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<ConversationParticipantResponseDto>> getByUserId(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        return ResponseEntity.ok(conversationParticipantInterface.getByUserId(id,currUserId));
    }

    @GetMapping("/conversation/{id}")
    public ResponseEntity<List<ConversationParticipantResponseDto>> getByConversationId(@PathVariable Long id ,@AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        return ResponseEntity.ok(conversationParticipantInterface.getByConversationId(id,currUserId));
    }

    @GetMapping("/user/{userId}/conversation/{conversationId}")
    public ResponseEntity<ConversationParticipantResponseDto> getByUserAndConversationId(@PathVariable Long userId, @PathVariable Long conversationId,@AuthenticationPrincipal CustomUserDetails currUser){
        Long currUserId = currUser.getId();
        return ResponseEntity.ok(conversationParticipantInterface.getByUserIdAndConversationId(userId, conversationId,currUserId));
    }

    @GetMapping("/unread")
    public ResponseEntity<UnReadCountResponseDto> getUnreadCount(
            @RequestParam Long conversationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(conversationParticipantInterface.getUnreadCount(conversationId, currentUser.getId()));
    }

    @PatchMapping("/read/{conversationId}")
    public ResponseEntity<Void>markAsRead(@PathVariable Long conversationId,@AuthenticationPrincipal CustomUserDetails currentUser){
        Long currUserId = currentUser.getId();
        conversationParticipantInterface.markAsRead(conversationId,currUserId);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails currUser) {
        Long currUserId = currUser.getId();
        boolean deleted = conversationParticipantInterface.leaveConversation(id,currUserId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }



}