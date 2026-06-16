package com.solventa.solventa_backend.leads.service;

import com.solventa.solventa_backend.customers.model.Customer;
import com.solventa.solventa_backend.customers.repository.CustomerRepository;
import com.solventa.solventa_backend.leads.dto.ConvertLeadResponse;
import com.solventa.solventa_backend.leads.dto.LeadRequest;
import com.solventa.solventa_backend.leads.dto.LeadResponse;
import com.solventa.solventa_backend.leads.dto.LeadStatusRequest;
import com.solventa.solventa_backend.leads.model.Lead;
import com.solventa.solventa_backend.leads.repository.LeadRepository;
import com.solventa.solventa_backend.shared.context.TenantContext;
import com.solventa.solventa_backend.shared.exception.BusinessException;
import com.solventa.solventa_backend.tenant.repository.CompanyRepository;
import com.solventa.solventa_backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository     leadRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository  companyRepository;
    private final UserRepository     userRepository;

    // ── Listar con filtros ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> findAll(
            Lead.LeadStatus status,
            Lead.LeadPriority priority,
            String q,
            int page,
            int size) {

        UUID tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        return leadRepository
                .findAllFiltered(tenantId, status, priority,
                        q != null && q.isBlank() ? null : q, pageable)
                .map(LeadResponse::from);
    }

    // ── Obtener por ID ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public LeadResponse findById(UUID id) {
        return LeadResponse.from(getLeadOrThrow(id));
    }

    // ── Crear ──────────────────────────────────────────────────────────────────
    @Transactional
    public LeadResponse create(LeadRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        var company = companyRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Empresa"));

        var lead = Lead.builder()
                .tenant(company)
                .name(req.getName())
                .company(req.getCompany())
                .email(req.getEmail())
                .phone(req.getPhone())
                .source(req.getSource())
                .priority(req.getPriority())
                .status(Lead.LeadStatus.NEW)
                .notes(req.getNotes())
                .build();

        if (req.getAssignedToId() != null) {
            var assignedTo = userRepository
                    .findById(UUID.fromString(req.getAssignedToId()))
                    .orElseThrow(() -> BusinessException.notFound("Usuario"));
            lead.setAssignedTo(assignedTo);
        }

        Lead saved = leadRepository.save(lead);
        log.info("Lead creado: {} | Tenant: {}", saved.getId(), tenantId);
        return LeadResponse.from(saved);
    }

    // ── Actualizar ─────────────────────────────────────────────────────────────
    @Transactional
    public LeadResponse update(UUID id, LeadRequest req) {
        Lead lead = getLeadOrThrow(id);

        lead.setName(req.getName());
        lead.setCompany(req.getCompany());
        lead.setEmail(req.getEmail());
        lead.setPhone(req.getPhone());
        lead.setSource(req.getSource());
        lead.setPriority(req.getPriority());
        lead.setNotes(req.getNotes());

        if (req.getAssignedToId() != null) {
            var assignedTo = userRepository
                    .findById(UUID.fromString(req.getAssignedToId()))
                    .orElseThrow(() -> BusinessException.notFound("Usuario"));
            lead.setAssignedTo(assignedTo);
        } else {
            lead.setAssignedTo(null);
        }

        return LeadResponse.from(leadRepository.save(lead));
    }

    // ── Cambiar estado ─────────────────────────────────────────────────────────
    @Transactional
    public LeadResponse changeStatus(UUID id, LeadStatusRequest req) {
        Lead lead = getLeadOrThrow(id);

        if (lead.getStatus() == Lead.LeadStatus.CONVERTED) {
            throw BusinessException.badRequest(
                    "Un lead convertido no puede cambiar de estado");
        }

        lead.setStatus(req.getStatus());
        return LeadResponse.from(leadRepository.save(lead));
    }

    // ── Convertir a cliente ────────────────────────────────────────────────────
    @Transactional
    public ConvertLeadResponse convertToCustomer(UUID id) {
        Lead lead = getLeadOrThrow(id);

        if (lead.getStatus() == Lead.LeadStatus.CONVERTED) {
            throw BusinessException.conflict(
                    "Este lead ya fue convertido a cliente");
        }

        // Crear el cliente a partir del lead
        Customer customer = Customer.builder()
                .tenant(lead.getTenant())
                .lead(lead)
                .assignedTo(lead.getAssignedTo())
                .name(lead.getName())
                .company(lead.getCompany())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .notes(lead.getNotes())
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        // Marcar el lead como convertido
        lead.setStatus(Lead.LeadStatus.CONVERTED);
        leadRepository.save(lead);

        log.info("Lead {} convertido a cliente {}", id, savedCustomer.getId());

        return new ConvertLeadResponse(
                savedCustomer.getId(),
                "Lead convertido a cliente exitosamente"
        );
    }

    // ── Eliminar (soft delete) ─────────────────────────────────────────────────
    @Transactional
    public void delete(UUID id) {
        Lead lead = getLeadOrThrow(id);
        lead.setDeletedAt(OffsetDateTime.now());
        leadRepository.save(lead);
        log.info("Lead eliminado (soft): {}", id);
    }

    // ── Helper privado ─────────────────────────────────────────────────────────
    private Lead getLeadOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return leadRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Lead"));
    }
}