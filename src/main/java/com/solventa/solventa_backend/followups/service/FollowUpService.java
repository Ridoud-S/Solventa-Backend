package com.solventa.solventa_backend.followups.service;

import com.solventa.solventa_backend.customers.repository.CustomerRepository;
import com.solventa.solventa_backend.followups.dto.FollowUpRequest;
import com.solventa.solventa_backend.followups.dto.FollowUpResponse;
import com.solventa.solventa_backend.followups.model.FollowUp;
import com.solventa.solventa_backend.followups.repository.FollowUpRepository;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final LeadRepository     leadRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository  companyRepository;
    private final UserRepository     userRepository;

    // ── Listar historial de una entidad ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<FollowUpResponse> findByEntity(FollowUp.EntityType entityType, UUID entityId) {
        UUID tenantId = TenantContext.getTenantId();

        validateEntityExists(entityType, entityId, tenantId);

        return followUpRepository
                .findByTenantIdAndEntityTypeAndEntityIdOrderByInteractionDateDesc(
                        tenantId, entityType, entityId)
                .stream()
                .map(FollowUpResponse::from)
                .toList();
    }

    // ── Crear seguimiento ──────────────────────────────────────────────────────
    @Transactional
    public FollowUpResponse create(FollowUpRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        validateEntityExists(req.getEntityType(), req.getEntityId(), tenantId);

        var company = companyRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Empresa"));

        var user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("Usuario"));

        var followUp = FollowUp.builder()
                .tenant(company)
                .user(user)
                .entityType(req.getEntityType())
                .entityId(req.getEntityId())
                .type(req.getType())
                .interactionDate(req.getInteractionDate())
                .notes(req.getNotes())
                .result(req.getResult())
                .build();

        FollowUp saved = followUpRepository.save(followUp);
        log.info("Seguimiento creado: {} | {} {} | Tenant: {}",
                saved.getId(), req.getEntityType(), req.getEntityId(), tenantId);

        return FollowUpResponse.from(saved);
    }

    // ── Eliminar (hard delete — los seguimientos no llevan soft delete) ────────
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();

        FollowUp followUp = followUpRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Seguimiento"));

        followUpRepository.delete(followUp);
        log.info("Seguimiento eliminado: {} | Tenant: {}", id, tenantId);
    }

    // ── Validar que la entidad referenciada existe y pertenece al tenant ───────
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
}