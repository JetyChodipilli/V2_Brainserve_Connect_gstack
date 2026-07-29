package com.brainserve.appointment.availability.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "availability_rules")
public class AvailabilityRule {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID employeeId;
    @Column(nullable = false)
    private short dayOfWeek;
    @Column(nullable = false)
    private LocalTime startsAt;
    @Column(nullable = false)
    private LocalTime endsAt;
    @Column(nullable = false)
    private int slotMinutes;
    @Column(nullable = false)
    private int bufferMinutes;
    @Column(nullable = false)
    private boolean active;
    @Version
    private long version;

    protected AvailabilityRule() {
    }

    public AvailabilityRule(
            UUID employeeId,
            DayOfWeek dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            int slotMinutes,
            int bufferMinutes) {
        if (!startsAt.isBefore(endsAt)) throw new IllegalArgumentException("Availability start must be before end");
        this.id = UUID.randomUUID();
        this.employeeId = employeeId;
        this.dayOfWeek = (short) dayOfWeek.getValue();
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.slotMinutes = slotMinutes;
        this.bufferMinutes = bufferMinutes;
        this.active = true;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public DayOfWeek getDayOfWeek() { return DayOfWeek.of(dayOfWeek); }
    public LocalTime getStartsAt() { return startsAt; }
    public LocalTime getEndsAt() { return endsAt; }
    public int getSlotMinutes() { return slotMinutes; }
    public int getBufferMinutes() { return bufferMinutes; }
}
