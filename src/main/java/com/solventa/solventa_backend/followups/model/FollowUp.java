package com.solventa.solventa_backend.followups.model;

import com.solventa.solventa_backend.tenant.model.Company;
import com.solventa.solventa_backend.users.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "follow_ups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Company tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "entity_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FollowUpType type;

    @Column(name = "interaction_date", nullable = false)
    private OffsetDateTime interactionDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String result;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Enums ──────────────────────────────────────────────────────────────────
    public enum EntityType {
        LEAD, CUSTOMER
    }

    public enum FollowUpType {
        CALL, EMAIL, MEETING, WHATSAPP, OTHER
    }
}