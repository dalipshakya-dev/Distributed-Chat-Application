package com.project.Chat_Application.Repositories;

import com.project.Chat_Application.Entities.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant,Long> {
    Optional<ConversationParticipant>findByUserIdAndConversationId(Long userId,Long conversationId);
    List<ConversationParticipant>findByUserId(Long userId);
    List<ConversationParticipant>findByConversationId(Long conversationId);
}
