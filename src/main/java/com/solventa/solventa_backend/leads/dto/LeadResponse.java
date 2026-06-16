package com.solventa.solventa_backend.leads.dto;

import com.solventa.solventa_backend.leads.model.Lead;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class LeadResponse {

    private UUID   id;
    private String name;
    private String company;
    private String email;
    private String phone;
    private Lead.LeadSource   source;
    private Lead.LeadStatus   status;
    private Lead.LeadPriority priority;
    private String notes;
    private AssignedUser assignedTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    @Builder
    public static class AssignedUser {
        private UUID   id;
        private String name;
        private String email;
    }

    // ── Mapper estático ────────────────────────────────────────────────────────
    public static LeadResponse from(Lead lead) {
        return LeadResponse.builder()
                .id(lead.getId())
                .name(lead.getName())
                .company(lead.getCompany())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .source(lead.getSource())
                .status(lead.getStatus())
                .priority(lead.getPriority())
                .notes(lead.getNotes())
                .assignedTo(lead.getAssignedTo() != null
                        ? AssignedUser.builder()
                        .id(lead.getAssignedTo().getId())
                        .name(lead.getAssignedTo().getName())
                        .email(lead.getAssignedTo().getEmail())
                        .build()
                        : null)
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }
}