package com.project.Chat_Application.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_Id", "conversation_Id"}
        )
)
@Entity
public class ConversationParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_Id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "conversation_Id")
    private Conversation conversation;


    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;

}
