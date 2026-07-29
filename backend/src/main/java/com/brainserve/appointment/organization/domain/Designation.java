package com.brainserve.appointment.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "designations")
public class Designation {
    @Id
    private UUID id;
    private UUID departmentId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private int level;
    @Column(nullable = false)
    private boolean active;
    @Version
    private long version;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Designation() {
    }

    public Designation(UUID departmentId, String name, int level) {
        this.id = UUID.randomUUID();
        this.departmentId = departmentId;
        this.name = name.trim();
        this.level = level;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDepartmentId() { return departmentId; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public boolean isActive() { return active; }
}
