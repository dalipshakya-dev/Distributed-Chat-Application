package com.project.Chat_Application.Repositories;

import com.project.Chat_Application.Entities.Conversation;
import com.project.Chat_Application.Entities.ConversationParticipant;
import com.project.Chat_Application.Entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation,Long> {
    // In ConversationRepository
    Page<Conversation> findByIdIn(List<Long> ids, Pageable pageable);
}
