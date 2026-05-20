package com.example.discountservice.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discount_campaigns")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CampaignEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID sellerId;
    private String name;
    private String description;
    @Column(name = "campaign_type") private String campaignType;
    private String status;
    private Instant startDate;
    private Instant endDate;
    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp private Instant updatedAt;
}

