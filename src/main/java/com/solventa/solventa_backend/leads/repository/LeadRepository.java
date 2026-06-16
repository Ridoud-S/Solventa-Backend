package com.solventa.solventa_backend.leads.repository;

import com.solventa.solventa_backend.leads.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    // ── Buscar por ID solo si pertenece al tenant y no está eliminado ──────────
    @EntityGraph(attributePaths = {"assignedTo", "tenant"})
    Optional<Lead> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // ── Listado paginado con filtros opcionales ────────────────────────────────
    @EntityGraph(attributePaths = {"assignedTo"})
    @Query("""
    SELECT l FROM Lead l
    WHERE l.tenant.id = :tenantId
      AND l.deletedAt IS NULL
      AND (:status   IS NULL OR l.status   = :status)
      AND (:priority IS NULL OR l.priority = :priority)
      AND (
          :q IS NULL
          OR LOWER(CAST(l.name    AS string)) LIKE LOWER(CAST(CONCAT('%', :q, '%') AS string))
          OR LOWER(CAST(l.company AS string)) LIKE LOWER(CAST(CONCAT('%', :q, '%') AS string))
          OR LOWER(CAST(l.email   AS string)) LIKE LOWER(CAST(CONCAT('%', :q, '%') AS string))
      )
    ORDER BY l.createdAt DESC
    """)
    Page<Lead> findAllFiltered(
            @Param("tenantId")  UUID tenantId,
            @Param("status")    Lead.LeadStatus status,
            @Param("priority")  Lead.LeadPriority priority,
            @Param("q")         String q,
            Pageable pageable);

    // ── Contar leads activos del tenant (para dashboard) ──────────────────────
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    // ── Contar por estado (para dashboard) ────────────────────────────────────
    long countByTenantIdAndStatusAndDeletedAtIsNull(
            UUID tenantId, Lead.LeadStatus status);
}