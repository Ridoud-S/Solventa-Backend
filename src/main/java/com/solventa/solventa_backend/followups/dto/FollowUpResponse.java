package com.solventa.solventa_backend.followups.dto;

import com.solventa.solventa_backend.followups.model.FollowUp;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class FollowUpResponse {

    private UUID    id;
    private FollowUp.EntityType   entityType;
    private UUID    entityId;
    private FollowUp.FollowUpType type;
    private OffsetDateTime interactionDate;
    private String  notes;
    private String  result;
    private UserInfo user;
    private OffsetDateTime createdAt;

    @Data
    @Builder
    public static class UserInfo {
        private UUID   id;
        private String name;
    }

    // ── Mapper estático ────────────────────────────────────────────────────────
    public static FollowUpResponse from(FollowUp f) {
        return FollowUpResponse.builder()
                .id(f.getId())
                .entityType(f.getEntityType())
                .entityId(f.getEntityId())
                .type(f.getType())
                .interactionDate(f.getInteractionDate())
                .notes(f.getNotes())
                .result(f.getResult())
                .user(f.getUser() != null
                        ? UserInfo.builder()
                        .id(f.getUser().getId())
                        .name(f.getUser().getName())
                        .build()
                        : null)
                .createdAt(f.getCreatedAt())
                .build();
    }
}