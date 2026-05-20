package org.example.userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(
        name = "user_addresses",
        indexes = @Index(name = "idx_addresses_user", columnList = "user_id")
)
@Getter
@Setter
public class UserAddress {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String receiverName;
    private String receiverPhone;

    private String province;
    private String district;
    private String ward;
    private String addressLine;

    private Boolean isDefault = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}