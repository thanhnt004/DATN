package org.example.userservice.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "user_status_history")
@Getter
@Setter
public class UserStatusHistory {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus newStatus;

    private String reason;

    @Column(columnDefinition = "uuid")
    private UUID changedBy; // admin_id

    @CreationTimestamp
    private Instant changedAt;
}
