package com.solventa.solventa_backend.followups.dto;

import com.solventa.solventa_backend.followups.model.FollowUp;
import com.solventa.solventa_backend.followups.model.Reminder;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ReminderResponse {

    private UUID    id;
    private FollowUp.EntityType entityType;
    private UUID    entityId;
    private OffsetDateTime remindAt;
    private String  description;
    private boolean isDone;
    private UserInfo user;
    private OffsetDateTime createdAt;

    @Data
    @Builder
    public static class UserInfo {
        private UUID   id;
        private String name;
    }

    // ── Mapper estático ────────────────────────────────────────────────────────
    public static ReminderResponse from(Reminder r) {
        return ReminderResponse.builder()
                .id(r.getId())
                .entityType(r.getEntityType())
                .entityId(r.getEntityId())
                .remindAt(r.getRemindAt())
                .description(r.getDescription())
                .isDone(r.isDone())
                .user(r.getUser() != null
                        ? UserInfo.builder()
                        .id(r.getUser().getId())
                        .name(r.getUser().getName())
                        .build()
                        : null)
                .createdAt(r.getCreatedAt())
                .build();
    }
}