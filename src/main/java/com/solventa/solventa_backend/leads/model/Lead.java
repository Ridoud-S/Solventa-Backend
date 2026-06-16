package com.solventa.solventa_backend.leads.model;

import com.solventa.solventa_backend.tenant.model.Company;
import com.solventa.solventa_backend.users.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Company tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 200)
    private String company;

    @Column(length = 200)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LeadSource source = LeadSource.OTHER;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LeadPriority priority = LeadPriority.MEDIUM;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── Soft delete ────────────────────────────────────────────────────────────
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Helper ─────────────────────────────────────────────────────────────────
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // ── Enums ──────────────────────────────────────────────────────────────────
    public enum LeadSource {
        WHATSAPP, EMAIL, REFERRAL, WEBSITE, PHONE, OTHER
    }

    public enum LeadStatus {
        NEW, CONTACTED, QUALIFIED, CONVERTED, DISCARDED
    }

    public enum LeadPriority {
        LOW, MEDIUM, HIGH
    }
}