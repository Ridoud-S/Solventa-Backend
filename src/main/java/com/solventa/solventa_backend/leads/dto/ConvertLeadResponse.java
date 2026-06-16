package com.solventa.solventa_backend.leads.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ConvertLeadResponse {
    private UUID customerId;
    private String message;
}