package com.solventa.solventa_backend.followups.repository;

import com.solventa.solventa_backend.followups.model.FollowUp;
import com.solventa.solventa_backend.followups.model.Reminder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    // ── Recordatorios de una entidad (Lead o Customer) ──────────────────────────
    @EntityGraph(attributePaths = {"user"})
    List<Reminder> findByTenantIdAndEntityTypeAndEntityIdOrderByRemindAtAsc(
            UUID tenantId, FollowUp.EntityType entityType, UUID entityId);

    // ── Buscar por ID validando tenant ──────────────────────────────────────────
    Optional<Reminder> findByIdAndTenantId(UUID id, UUID tenantId);

    // ── Recordatorios pendientes de HOY para el usuario en sesión ───────────────
    @EntityGraph(attributePaths = {"user"})
    @Query("""
        SELECT r FROM Reminder r
        WHERE r.tenant.id = :tenantId
          AND r.user.id   = :userId
          AND r.isDone    = false
          AND r.remindAt BETWEEN :start AND :end
        ORDER BY r.remindAt ASC
        """)
    List<Reminder> findTodayForUser(
            @Param("tenantId") UUID tenantId,
            @Param("userId")   UUID userId,
            @Param("start")    OffsetDateTime start,
            @Param("end")      OffsetDateTime end);

    // ── Contar recordatorios pendientes de hoy (para dashboard) ─────────────────
    @Query("""
        SELECT COUNT(r) FROM Reminder r
        WHERE r.tenant.id = :tenantId
          AND r.user.id   = :userId
          AND r.isDone    = false
          AND r.remindAt BETWEEN :start AND :end
        """)
    long countTodayForUser(
            @Param("tenantId") UUID tenantId,
            @Param("userId")   UUID userId,
            @Param("start")    OffsetDateTime start,
            @Param("end")      OffsetDateTime end);
}