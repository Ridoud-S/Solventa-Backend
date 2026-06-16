package com.solventa.solventa_backend.quotes.service;

import com.solventa.solventa_backend.customers.repository.CustomerRepository;
import com.solventa.solventa_backend.quotes.dto.*;
import com.solventa.solventa_backend.quotes.model.Quote;
import com.solventa.solventa_backend.quotes.model.QuoteLine;
import com.solventa.solventa_backend.quotes.model.QuoteStatusHistory;
import com.solventa.solventa_backend.quotes.repository.QuoteRepository;
import com.solventa.solventa_backend.quotes.repository.QuoteStatusHistoryRepository;
import com.solventa.solventa_backend.shared.context.TenantContext;
import com.solventa.solventa_backend.shared.exception.BusinessException;
import com.solventa.solventa_backend.shared.util.SecurityUtils;
import com.solventa.solventa_backend.tenant.repository.CompanyRepository;
import com.solventa.solventa_backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    // ── Listar con filtros ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<QuoteResponse> findAll(
            Quote.QuoteStatus status, UUID customerId, String q, int page, int size) {

        UUID tenantId = TenantContext.getTenantId();

        return quoteRepository
                .findAllFiltered(tenantId, status, customerId,
                        q != null && q.isBlank() ? null : q,
                        PageRequest.of(page, size))
                .map(QuoteResponse::from);
    }

    // ── Obtener por ID ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public QuoteResponse findById(UUID id) {
        return QuoteResponse.from(getQuoteOrThrow(id));
    }

    // ── Historial de estados ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<QuoteStatusHistoryResponse> getStatusHistory(UUID id) {
        getQuoteOrThrow(id); // valida que la cotización pertenece al tenant
        return historyRepository.findByQuoteIdOrderByChangedAtDesc(id)
                .stream()
                .map(QuoteStatusHistoryResponse::from)
                .toList();
    }

    // ── Crear ──────────────────────────────────────────────────────────────────
    @Transactional
    public QuoteResponse create(QuoteRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        var company = companyRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Empresa"));

        var customer = customerRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(req.getCustomerId(), tenantId)
                .orElseThrow(() -> BusinessException.notFound("Cliente"));

        var createdBy = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("Usuario"));

        var quote = Quote.builder()
                .tenant(company)
                .customer(customer)
                .createdBy(createdBy)
                .title(req.getTitle())
                .status(Quote.QuoteStatus.DRAFT)
                .discountPct(orZero(req.getDiscountPct()))
                .taxPct(req.getTaxPct() != null ? req.getTaxPct() : new BigDecimal("16.00"))
                .issuedAt(req.getIssuedAt() != null ? req.getIssuedAt() : LocalDate.now())
                .expiresAt(req.getExpiresAt())
                .notes(req.getNotes())
                .build();

        attachLines(quote, req.getLines());
        quote.recalculateTotals();

        Quote saved = quoteRepository.save(quote);
        log.info("Cotización creada: {} | Cliente: {} | Total: {} | Tenant: {}",
                saved.getId(), customer.getId(), saved.getTotal(), tenantId);

        return QuoteResponse.from(saved);
    }

    // ── Actualizar (solo si está en DRAFT) ─────────────────────────────────────
    @Transactional
    public QuoteResponse update(UUID id, QuoteRequest req) {
        Quote quote = getQuoteOrThrow(id);

        if (quote.getStatus() != Quote.QuoteStatus.DRAFT) {
            throw BusinessException.badRequest(
                    "Solo se pueden editar cotizaciones en estado Borrador");
        }

        UUID tenantId = TenantContext.getTenantId();

        if (!quote.getCustomer().getId().equals(req.getCustomerId())) {
            var customer = customerRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(req.getCustomerId(), tenantId)
                    .orElseThrow(() -> BusinessException.notFound("Cliente"));
            quote.setCustomer(customer);
        }

        quote.setTitle(req.getTitle());
        quote.setDiscountPct(orZero(req.getDiscountPct()));
        quote.setTaxPct(req.getTaxPct() != null ? req.getTaxPct() : new BigDecimal("16.00"));
        if (req.getIssuedAt() != null) quote.setIssuedAt(req.getIssuedAt());
        quote.setExpiresAt(req.getExpiresAt());
        quote.setNotes(req.getNotes());

        // Reemplazar líneas — orphanRemoval=true elimina las anteriores
        quote.getLines().clear();
        attachLines(quote, req.getLines());
        quote.recalculateTotals();

        return QuoteResponse.from(quoteRepository.save(quote));
    }

    // ── Cambiar estado ─────────────────────────────────────────────────────────
    @Transactional
    public QuoteResponse changeStatus(UUID id, QuoteStatusRequest req) {
        Quote quote = getQuoteOrThrow(id);

        Quote.QuoteStatus current = quote.getStatus();
        Quote.QuoteStatus next    = req.getStatus();

        validateTransition(current, next);

        var changedBy = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("Usuario"));

        quote.setStatus(next);
        Quote saved = quoteRepository.save(quote);

        historyRepository.save(QuoteStatusHistory.builder()
                .quote(saved)
                .oldStatus(current)
                .newStatus(next)
                .changedBy(changedBy)
                .build());

        log.info("Cotización {} cambió de {} a {}", id, current, next);

        return QuoteResponse.from(saved);
    }

    // ── Eliminar (solo si está en DRAFT) ───────────────────────────────────────
    @Transactional
    public void delete(UUID id) {
        Quote quote = getQuoteOrThrow(id);

        if (quote.getStatus() != Quote.QuoteStatus.DRAFT) {
            throw BusinessException.badRequest(
                    "Solo se pueden eliminar cotizaciones en estado Borrador");
        }

        quote.setDeletedAt(OffsetDateTime.now());
        quoteRepository.save(quote);
        log.info("Cotización eliminada (soft): {}", id);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private Quote getQuoteOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return quoteRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Cotización"));
    }

    private void attachLines(Quote quote, List<QuoteLineRequest> lineRequests) {
        int order = 0;
        for (var lr : lineRequests) {
            var line = QuoteLine.builder()
                    .quote(quote)
                    .description(lr.getDescription())
                    .quantity(lr.getQuantity())
                    .unitPrice(lr.getUnitPrice())
                    .sortOrder(order++)
                    .build();
            line.calculateSubtotal();
            quote.getLines().add(line);
        }
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ── Validación de transiciones de estado ────────────────────────────────────
    private void validateTransition(Quote.QuoteStatus from, Quote.QuoteStatus to) {
        if (from == to) {
            throw BusinessException.badRequest(
                    "La cotización ya está en estado " + to);
        }

        boolean valid = switch (from) {
            case DRAFT -> to == Quote.QuoteStatus.SENT;
            case SENT  -> to == Quote.QuoteStatus.WON
                    || to == Quote.QuoteStatus.LOST
                    || to == Quote.QuoteStatus.EXPIRED;
            case WON, LOST, EXPIRED -> false; // estados terminales
        };

        if (!valid) {
            throw BusinessException.badRequest(
                    "Transición inválida: no se puede cambiar de " + from + " a " + to);
        }
    }
}