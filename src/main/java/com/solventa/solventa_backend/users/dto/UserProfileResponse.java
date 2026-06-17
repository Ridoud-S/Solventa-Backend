package com.solventa.solventa_backend.users.dto;

import com.solventa.solventa_backend.users.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {

    private UUID   id;
    private String name;
    private String email;
    private String role;
    private boolean isActive;
    private UUID   tenantId;
    private String companyName;
    private OffsetDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .tenantId(user.getTenant().getId())
                .companyName(user.getTenant().getName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}