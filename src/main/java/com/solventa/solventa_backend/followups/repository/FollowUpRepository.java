package com.solventa.solventa_backend.followups.repository;

import com.solventa.solventa_backend.followups.model.FollowUp;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, UUID> {

    // ── Historial de una entidad (Lead o Customer), más reciente primero ───────
    @EntityGraph(attributePaths = {"user"})
    List<FollowUp> findByTenantIdAndEntityTypeAndEntityIdOrderByInteractionDateDesc(
            UUID tenantId, FollowUp.EntityType entityType, UUID entityId);

    // ── Buscar por ID validando tenant ──────────────────────────────────────────
    Optional<FollowUp> findByIdAndTenantId(UUID id, UUID tenantId);

    // ── Contar interacciones del tenant (para dashboard futuro) ────────────────
    long countByTenantId(UUID tenantId);
}