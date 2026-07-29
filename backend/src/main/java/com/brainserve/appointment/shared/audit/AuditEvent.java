package com.brainserve.appointment.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String eventType;
    private UUID actorUserId;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String entityType;
    private String entityId;
    private String reason;
    private String correlationId;
    @Column(nullable = false)
    private String sensitivity;
    private String ipAddress;
    @Column(nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID actorUserId, String action, String entityType, String entityId, String reason, String sensitivity) {
        this.id = UUID.randomUUID();
        this.eventType = entityType + "_" + action;
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.reason = reason;
        this.correlationId = org.slf4j.MDC.get("correlationId");
        this.sensitivity = sensitivity;
        this.occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public UUID getActorUserId() { return actorUserId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getReason() { return reason; }
    public String getCorrelationId() { return correlationId; }
    public String getSensitivity() { return sensitivity; }
    public Instant getOccurredAt() { return occurredAt; }
}
