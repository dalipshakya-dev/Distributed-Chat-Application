package com.project.Chat_Application.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.Chat_Application.Entities.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@AllArgsConstructor
@NoArgsConstructor
@Data

@Entity
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "conversation_Id",nullable = false)
    private Conversation conversation;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "sender_id")
    private User sender;


    private String content;
    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    private LocalDateTime createdAt;


}
