package com.solventa.solventa_backend.customers.repository;

import com.solventa.solventa_backend.customers.model.Customer;
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
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    // ── Buscar por ID solo si pertenece al tenant y no está eliminado ──────────
    @EntityGraph(attributePaths = {"assignedTo", "lead", "tenant"})
    Optional<Customer> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // ── Listado paginado con búsqueda ──────────────────────────────────────────
    @EntityGraph(attributePaths = {"assignedTo", "lead"})
    @Query("""
        SELECT c FROM Customer c
        WHERE c.tenant.id = :tenantId
          AND c.deletedAt IS NULL
          AND (:q IS NULL
               OR LOWER(CAST(c.name    AS string)) LIKE LOWER(CAST(CONCAT('%', :q, '%') AS string))
               OR LOWER(CAST(c.company AS string)) LIKE LOWER(CAST(CONCAT('%', :q, '%') AS string))
               OR LOWER(CAST(c.email   AS string)) LIKE LOWER(CAST(CONCAT('%', :q, '%') AS string)))
        ORDER BY c.createdAt DESC
        """)
    Page<Customer> findAllFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("q")        String q,
            Pageable pageable);

    // ── Contar clientes activos del tenant (para dashboard) ────────────────────
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    // ── Verificar email duplicado dentro del tenant ─────────────────────────────
    boolean existsByEmailAndTenantIdAndDeletedAtIsNull(String email, UUID tenantId);
}