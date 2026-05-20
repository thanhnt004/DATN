package org.example.userservice.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "user_devices")
@Getter
@Setter
public class UserDevice {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String deviceId;

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    private String os;
    private String appVersion;

    private Instant lastLoginAt;
}

