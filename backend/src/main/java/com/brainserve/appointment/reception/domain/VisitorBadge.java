package com.brainserve.appointment.reception.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "visitor_badges")
public class VisitorBadge {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String badgeNumber;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false)
    private boolean allocated;
    @Version
    private long version;

    protected VisitorBadge() {
    }

    public void allocate() {
        if (!active || allocated) throw new IllegalStateException("Badge is unavailable");
        allocated = true;
    }

    public void release() {
        allocated = false;
    }

    public UUID getId() { return id; }
    public String getBadgeNumber() { return badgeNumber; }
    public boolean isAllocated() { return allocated; }
}
