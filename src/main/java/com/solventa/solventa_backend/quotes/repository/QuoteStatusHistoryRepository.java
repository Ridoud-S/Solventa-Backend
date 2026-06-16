package com.solventa.solventa_backend.quotes.repository;

import com.solventa.solventa_backend.quotes.model.QuoteStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuoteStatusHistoryRepository extends JpaRepository<QuoteStatusHistory, UUID> {

    @EntityGraph(attributePaths = {"changedBy"})
    List<QuoteStatusHistory> findByQuoteIdOrderByChangedAtDesc(UUID quoteId);
}