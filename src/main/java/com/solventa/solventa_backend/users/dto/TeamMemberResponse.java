package com.solventa.solventa_backend.users.dto;

import com.solventa.solventa_backend.users.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class TeamMemberResponse {

    private UUID   id;
    private String name;
    private String email;
    private String role;
    private boolean isActive;
    private boolean invitationAccepted;
    private OffsetDateTime createdAt;

    public static TeamMemberResponse from(User user) {
        return TeamMemberResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .invitationAccepted(user.isInvitationAccepted())
                .createdAt(user.getCreatedAt())
                .build();
    }
}