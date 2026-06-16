package com.solventa.solventa_backend.tenant.repository;

import com.solventa.solventa_backend.tenant.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
}