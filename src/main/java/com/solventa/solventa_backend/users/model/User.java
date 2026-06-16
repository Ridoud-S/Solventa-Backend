package com.solventa.solventa_backend.users.model;

import com.solventa.solventa_backend.tenant.model.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_users_email_tenant",
                columnNames = {"tenant_id", "email"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Company tenant;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.SELLER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ── Invitación ─────────────────────────────────────────────────────────────
    @Column(name = "invitation_token")
    private String invitationToken;

    @Column(name = "invitation_expires_at")
    private OffsetDateTime invitationExpiresAt;

    @Column(name = "invitation_accepted", nullable = false)
    @Builder.Default
    private boolean invitationAccepted = false;

    // ── Reset de contraseña ────────────────────────────────────────────────────
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private OffsetDateTime resetTokenExpiresAt;

    // ── Timestamps ─────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}