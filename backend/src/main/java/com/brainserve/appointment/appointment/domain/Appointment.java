package com.brainserve.appointment.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String referenceNumber;
    @Column(unique = true)
    private String idempotencyKey;
    @Column(nullable = false)
    private UUID visitorId;
    @Column(nullable = false)
    private UUID hostEmployeeId;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false)
    private String purpose;
    @Column(nullable = false)
    private Instant startsAt;
    @Column(nullable = false)
    private Instant endsAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;
    private String verificationHash;
    private String qrTokenHash;
    private UUID decisionBy;
    private Instant decisionAt;
    private String decisionRemarks;
    @Version
    private long version;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Appointment() {
    }

    public Appointment(
            String referenceNumber,
            String idempotencyKey,
            UUID visitorId,
            UUID hostEmployeeId,
            String type,
            String purpose,
            Instant startsAt,
            Instant endsAt,
            String verificationHash) {
        if (!startsAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Appointment start must be in the future");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Appointment end must be after start");
        }
        this.id = UUID.randomUUID();
        this.referenceNumber = referenceNumber;
        this.idempotencyKey = idempotencyKey;
        this.visitorId = visitorId;
        this.hostEmployeeId = hostEmployeeId;
        this.type = type;
        this.purpose = purpose;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = AppointmentStatus.PENDING_VERIFICATION;
        this.verificationHash = verificationHash;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void verify() {
        transition(AppointmentStatus.PENDING_APPROVAL);
        verificationHash = null;
    }

    public void decide(AppointmentStatus target, UUID actor, String remarks) {
        if (target != AppointmentStatus.APPROVED && target != AppointmentStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must approve or reject");
        }
        transition(target);
        decisionBy = actor;
        decisionAt = Instant.now();
        decisionRemarks = remarks;
    }

    public void transition(AppointmentStatus target) {
        status.requireTransitionTo(target);
        status = target;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getVisitorId() { return visitorId; }
    public UUID getHostEmployeeId() { return hostEmployeeId; }
    public String getType() { return type; }
    public String getPurpose() { return purpose; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public AppointmentStatus getStatus() { return status; }
    public String getVerificationHash() { return verificationHash; }
    public UUID getDecisionBy() { return decisionBy; }
    public Instant getDecisionAt() { return decisionAt; }
    public String getDecisionRemarks() { return decisionRemarks; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
