package com.solventa.solventa_backend.quotes.repository;

import com.solventa.solventa_backend.quotes.model.Quote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    // ── Detalle completo con líneas, cliente y creador ──────────────────────────
    @EntityGraph(attributePaths = {"customer", "createdBy", "lines"})
    Optional<Quote> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // ── Listado paginado con filtros ────────────────────────────────────────────
    @EntityGraph(attributePaths = {"customer", "createdBy"})
    @Query("""
        SELECT q FROM Quote q
        WHERE q.tenant.id = :tenantId
          AND q.deletedAt IS NULL
          AND (:status     IS NULL OR q.status = :status)
          AND (:customerId IS NULL OR q.customer.id = :customerId)
          AND (:q IS NULL OR LOWER(CAST(q.title AS string)) LIKE LOWER(CAST(CONCAT('%', :q, '%') AS string)))
        ORDER BY q.createdAt DESC
        """)
    Page<Quote> findAllFiltered(
            @Param("tenantId")   UUID tenantId,
            @Param("status")     Quote.QuoteStatus status,
            @Param("customerId") UUID customerId,
            @Param("q")          String q,
            Pageable pageable);

    // ── Para el job de vencimiento automático (global, sin filtro de tenant) ───
    List<Quote> findAllByStatusAndExpiresAtBeforeAndDeletedAtIsNull(
            Quote.QuoteStatus status, LocalDate date);

    // ── Para el dashboard: conteo por estado ────────────────────────────────────
    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, Quote.QuoteStatus status);

    // ── Agregación para el dashboard: count + sum(total) agrupado por estado ──────
    @Query("""
    SELECT q.status AS status, COUNT(q) AS count, COALESCE(SUM(q.total), 0) AS total
    FROM Quote q
    WHERE q.tenant.id = :tenantId
      AND q.deletedAt IS NULL
    GROUP BY q.status
    """)
    List<QuoteStatusAggregation> findStatusAggregations(@Param("tenantId") UUID tenantId);
}