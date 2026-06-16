package com.solventa.solventa_backend.customers.service;

import com.solventa.solventa_backend.customers.dto.CustomerRequest;
import com.solventa.solventa_backend.customers.dto.CustomerResponse;
import com.solventa.solventa_backend.customers.model.Customer;
import com.solventa.solventa_backend.customers.repository.CustomerRepository;
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
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CompanyRepository  companyRepository;
    private final UserRepository     userRepository;

    // ── Listar con búsqueda ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(String q, int page, int size) {
        UUID tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        return customerRepository
                .findAllFiltered(tenantId,
                        q != null && q.isBlank() ? null : q, pageable)
                .map(CustomerResponse::from);
    }

    // ── Obtener por ID ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        return CustomerResponse.from(getCustomerOrThrow(id));
    }

    // ── Crear ──────────────────────────────────────────────────────────────────
    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        var company = companyRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Empresa"));

        if (req.getEmail() != null &&
                customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNull(
                        req.getEmail(), tenantId)) {
            throw BusinessException.conflict(
                    "Ya existe un cliente con ese correo electrónico");
        }

        var customer = Customer.builder()
                .tenant(company)
                .name(req.getName())
                .company(req.getCompany())
                .email(req.getEmail())
                .phone(req.getPhone())
                .rfc(req.getRfc())
                .address(req.getAddress())
                .notes(req.getNotes())
                .build();

        if (req.getAssignedToId() != null) {
            var assignedTo = userRepository
                    .findById(UUID.fromString(req.getAssignedToId()))
                    .orElseThrow(() -> BusinessException.notFound("Usuario"));
            customer.setAssignedTo(assignedTo);
        }

        Customer saved = customerRepository.save(customer);
        log.info("Cliente creado: {} | Tenant: {}", saved.getId(), tenantId);
        return CustomerResponse.from(saved);
    }

    // ── Actualizar ─────────────────────────────────────────────────────────────
    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest req) {
        Customer customer = getCustomerOrThrow(id);
        UUID tenantId = TenantContext.getTenantId();

        // Si cambia el email, verificar que no choque con otro cliente
        if (req.getEmail() != null && !req.getEmail().equals(customer.getEmail())
                && customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNull(
                req.getEmail(), tenantId)) {
            throw BusinessException.conflict(
                    "Ya existe un cliente con ese correo electrónico");
        }

        customer.setName(req.getName());
        customer.setCompany(req.getCompany());
        customer.setEmail(req.getEmail());
        customer.setPhone(req.getPhone());
        customer.setRfc(req.getRfc());
        customer.setAddress(req.getAddress());
        customer.setNotes(req.getNotes());

        if (req.getAssignedToId() != null) {
            var assignedTo = userRepository
                    .findById(UUID.fromString(req.getAssignedToId()))
                    .orElseThrow(() -> BusinessException.notFound("Usuario"));
            customer.setAssignedTo(assignedTo);
        } else {
            customer.setAssignedTo(null);
        }

        return CustomerResponse.from(customerRepository.save(customer));
    }

    // ── Eliminar (soft delete) ─────────────────────────────────────────────────
    @Transactional
    public void delete(UUID id) {
        Customer customer = getCustomerOrThrow(id);
        customer.setDeletedAt(OffsetDateTime.now());
        customerRepository.save(customer);
        log.info("Cliente eliminado (soft): {}", id);
    }

    // ── Helper privado ─────────────────────────────────────────────────────────
    private Customer getCustomerOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return customerRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Cliente"));
    }
}