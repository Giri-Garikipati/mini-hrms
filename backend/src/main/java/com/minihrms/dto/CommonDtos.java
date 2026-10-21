package com.minihrms.dto;

import com.minihrms.model.AuditLog;

import java.time.Instant;
import java.util.List;

public final class CommonDtos {

    private CommonDtos() { }

    /** One page of results, e.g. audit logs 20 at a time. */
    public record PageDto<T>(List<T> items, int page, int totalPages, long totalItems) { }

    public record AuditLogDto(Long id, String actorName, String action, String entity,
                              Long entityId, String details, Instant createdAt) {

        public static AuditLogDto from(AuditLog a) {
            String actor = a.getActor() == null ? "System" : a.getActor().getName();
            return new AuditLogDto(a.getId(), actor, a.getAction(), a.getEntity(),
                    a.getEntityId(), a.getDetails(), a.getCreatedAt());
        }
    }
}
