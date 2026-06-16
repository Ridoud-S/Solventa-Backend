package com.solventa.solventa_backend.quotes.model;

import com.solventa.solventa_backend.customers.model.Customer;
import com.solventa.solventa_backend.tenant.model.Company;
import com.solventa.solventa_backend.users.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Company tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.DRAFT;

    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "tax_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxPct = new BigDecimal("16.00");

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private LocalDate issuedAt = LocalDate.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @OneToMany(
            mappedBy = "quote",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<QuoteLine> lines = new ArrayList<>();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Helpers ────────────────────────────────────────────────────────────────
    public boolean isDeleted()  { return deletedAt != null; }
    public boolean isExpired()  { return expiresAt != null && LocalDate.now().isAfter(expiresAt); }


    public void recalculateTotals() {
        BigDecimal sub = lines.stream()
                .map(QuoteLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal afterDiscount = sub.multiply(
                BigDecimal.ONE.subtract(discountPct.divide(new BigDecimal("100")))
        );

        this.subtotal = sub;
        this.total = afterDiscount
                .multiply(BigDecimal.ONE.add(taxPct.divide(new BigDecimal("100"))))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // ── Enum ───────────────────────────────────────────────────────────────────
    public enum QuoteStatus {
        DRAFT, SENT, WON, LOST, EXPIRED
    }
}