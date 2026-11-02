package com.project.Chat_Application.Entities;

import com.project.Chat_Application.Entities.enums.ConversationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ConversationType type;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "conversation")
    private List<Message> messages = new ArrayList<>();
}