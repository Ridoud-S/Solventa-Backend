package com.solventa.solventa_backend.tenant.dto;

import com.solventa.solventa_backend.tenant.model.Company;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CompanyResponse {

    private UUID   id;
    private String name;
    private String industry;
    private String rfc;
    private String logoUrl;
    private String plan;
    private String status;

    public static CompanyResponse from(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .industry(company.getIndustry())
                .rfc(company.getRfc())
                .logoUrl(company.getLogoUrl())
                .plan(company.getPlan().name())
                .status(company.getStatus().name())
                .build();
    }
}