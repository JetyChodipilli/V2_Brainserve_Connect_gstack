package com.brainserve.appointment.reception.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visit_access_records")
public class VisitAccessRecord {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private UUID appointmentId;
    @Column(nullable = false)
    private UUID visitorId;
    private UUID badgeId;
    private String entryGate;
    private String exitGate;
    private Instant checkedInAt;
    private Instant checkedOutAt;
    private UUID checkedInBy;
    private UUID checkedOutBy;
    private String overrideReason;
    @Version
    private long version;

    protected VisitAccessRecord() {
    }

    public VisitAccessRecord(UUID appointmentId, UUID visitorId, UUID badgeId, String entryGate, UUID actor, String overrideReason) {
        this.id = UUID.randomUUID();
        this.appointmentId = appointmentId;
        this.visitorId = visitorId;
        this.badgeId = badgeId;
        this.entryGate = entryGate;
        this.checkedInAt = Instant.now();
        this.checkedInBy = actor;
        this.overrideReason = overrideReason;
    }

    public void checkOut(String gate, UUID actor) {
        if (checkedOutAt != null) throw new IllegalStateException("Visitor is already checked out");
        checkedOutAt = Instant.now();
        exitGate = gate;
        checkedOutBy = actor;
    }

    public UUID getId() { return id; }
    public UUID getAppointmentId() { return appointmentId; }
    public UUID getVisitorId() { return visitorId; }
    public UUID getBadgeId() { return badgeId; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public Instant getCheckedOutAt() { return checkedOutAt; }
    public String getEntryGate() { return entryGate; }
    public String getExitGate() { return exitGate; }
}
