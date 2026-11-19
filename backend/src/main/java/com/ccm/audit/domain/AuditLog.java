package com.ccm.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id", updatable = false)
    private Long actorUserId;

    @Column(name = "actor_username", nullable = false, updatable = false, length = 100)
    private String actorUsername;

    @Column(name = "module", nullable = false, updatable = false, length = 50)
    private String module;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false, length = 64)
    private String entityId;

    @Column(name = "action", nullable = false, updatable = false, length = 50)
    private String action;

    @Column(name = "summary", nullable = false, updatable = false, length = 500)
    private String summary;

    @Column(name = "payload_json", columnDefinition = "json", updatable = false)
    private String payloadJson;

    protected AuditLog() {
    }

    public AuditLog(
            Long actorUserId,
            String actorUsername,
            String module,
            String entityType,
            String entityId,
            String action,
            String summary,
            String payloadJson
    ) {
        this.occurredAt = Instant.now();
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername == null || actorUsername.isBlank() ? "SYSTEM" : actorUsername;
        this.module = module;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.summary = summary;
        this.payloadJson = payloadJson;
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getModule() {
        return module;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getSummary() {
        return summary;
    }

    public String getPayloadJson() {
        return payloadJson;
    }
}
