package com.solventa.solventa_backend.customers.dto;

import com.solventa.solventa_backend.customers.model.Customer;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerResponse {

    private UUID   id;
    private String name;
    private String company;
    private String email;
    private String phone;
    private String rfc;
    private String address;
    private String notes;
    private UUID   leadId;
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
    public static CustomerResponse from(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .company(customer.getCompany())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .rfc(customer.getRfc())
                .address(customer.getAddress())
                .notes(customer.getNotes())
                .leadId(customer.getLead() != null ? customer.getLead().getId() : null)
                .assignedTo(customer.getAssignedTo() != null
                        ? AssignedUser.builder()
                        .id(customer.getAssignedTo().getId())
                        .name(customer.getAssignedTo().getName())
                        .email(customer.getAssignedTo().getEmail())
                        .build()
                        : null)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}