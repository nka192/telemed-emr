package com.nayoung.telemed.notification.entity;

import com.nayoung.telemed.enums.NotificationType;
import com.nayoung.telemed.users.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;

    private String recipient;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type; // EMAIL / SMS / PUSH

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private final LocalDateTime createdAt = LocalDateTime.now();
}
