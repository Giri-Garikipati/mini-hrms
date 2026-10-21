package com.minihrms.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One row in "audit_logs". Audit logs are only ever ADDED, never edited or deleted.
 * That's why there are no setters: once created, a log entry can't change.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Employee actor;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() { }

    public AuditLog(Employee actor, String action, String entity, Long entityId, String details) {
        this.actor = actor;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.details = details;
    }

    public Long getId() { return id; }
    public Employee getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntity() { return entity; }
    public Long getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
