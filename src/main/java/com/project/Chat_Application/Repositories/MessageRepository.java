package com.project.Chat_Application.Repositories;

import com.project.Chat_Application.Entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MessageRepository extends JpaRepository<Message,Long> {
    Page<Message> findByConversationId(Long conversationId, Pageable pageable);
    long countByConversationId(Long conversationId);
    long countByConversationIdAndCreatedAtAfter(Long conversationId, LocalDateTime timeStamp);
}
