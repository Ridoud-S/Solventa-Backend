package com.solventa.solventa_backend.followups.service;

import com.solventa.solventa_backend.customers.repository.CustomerRepository;
import com.solventa.solventa_backend.followups.dto.ReminderRequest;
import com.solventa.solventa_backend.followups.dto.ReminderResponse;
import com.solventa.solventa_backend.followups.model.FollowUp;
import com.solventa.solventa_backend.followups.model.Reminder;
import com.solventa.solventa_backend.followups.repository.ReminderRepository;
import com.solventa.solventa_backend.leads.repository.LeadRepository;
import com.solventa.solventa_backend.shared.context.TenantContext;
import com.solventa.solventa_backend.shared.exception.BusinessException;
import com.solventa.solventa_backend.shared.util.SecurityUtils;
import com.solventa.solventa_backend.tenant.repository.CompanyRepository;
import com.solventa.solventa_backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final LeadRepository     leadRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository  companyRepository;
    private final UserRepository     userRepository;

    // ── Listar recordatorios de una entidad ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ReminderResponse> findByEntity(FollowUp.EntityType entityType, UUID entityId) {
        UUID tenantId = TenantContext.getTenantId();

        validateEntityExists(entityType, entityId, tenantId);

        return reminderRepository
                .findByTenantIdAndEntityTypeAndEntityIdOrderByRemindAtAsc(
                        tenantId, entityType, entityId)
                .stream()
                .map(ReminderResponse::from)
                .toList();
    }

    // ── Recordatorios pendientes de HOY para el usuario en sesión ──────────────
    @Transactional(readOnly = true)
    public List<ReminderResponse> findTodayForCurrentUser() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = SecurityUtils.getCurrentUserId();

        var range = todayRange();

        return reminderRepository
                .findTodayForUser(tenantId, userId, range[0], range[1])
                .stream()
                .map(ReminderResponse::from)
                .toList();
    }

    // ── Crear recordatorio ──────────────────────────────────────────────────────
    @Transactional
    public ReminderResponse create(ReminderRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        validateEntityExists(req.getEntityType(), req.getEntityId(), tenantId);

        var company = companyRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Empresa"));

        var user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("Usuario"));

        var reminder = Reminder.builder()
                .tenant(company)
                .user(user)
                .entityType(req.getEntityType())
                .entityId(req.getEntityId())
                .remindAt(req.getRemindAt())
                .description(req.getDescription())
                .isDone(false)
                .build();

        Reminder saved = reminderRepository.save(reminder);
        log.info("Recordatorio creado: {} | {} {} | Tenant: {}",
                saved.getId(), req.getEntityType(), req.getEntityId(), tenantId);

        return ReminderResponse.from(saved);
    }

    // ── Marcar como completado ──────────────────────────────────────────────────
    @Transactional
    public ReminderResponse complete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();

        Reminder reminder = reminderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Recordatorio"));

        reminder.setDone(true);
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    // ── Eliminar ───────────────────────────────────────────────────────────────
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();

        Reminder reminder = reminderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Recordatorio"));

        reminderRepository.delete(reminder);
        log.info("Recordatorio eliminado: {} | Tenant: {}", id, tenantId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void validateEntityExists(FollowUp.EntityType type, UUID entityId, UUID tenantId) {
        boolean exists = switch (type) {
            case LEAD -> leadRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(entityId, tenantId)
                    .isPresent();
            case CUSTOMER -> customerRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(entityId, tenantId)
                    .isPresent();
        };

        if (!exists) {
            throw BusinessException.notFound(
                    type == FollowUp.EntityType.LEAD ? "Lead" : "Cliente");
        }
    }

    /** Retorna [inicio del día, inicio del día siguiente] en la zona del servidor. */
    private OffsetDateTime[] todayRange() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime start = now.toLocalDate().atStartOfDay(now.getOffset()).toOffsetDateTime();
        OffsetDateTime end   = start.plusDays(1);
        return new OffsetDateTime[]{start, end};
    }
}