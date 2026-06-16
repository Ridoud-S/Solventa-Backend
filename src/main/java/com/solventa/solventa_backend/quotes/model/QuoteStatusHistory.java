package com.solventa.solventa_backend.quotes.model;

import com.solventa.solventa_backend.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "quote_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "old_status", length = 20)
    @Enumerated(EnumType.STRING)
    private Quote.QuoteStatus oldStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Quote.QuoteStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime changedAt = OffsetDateTime.now();
}