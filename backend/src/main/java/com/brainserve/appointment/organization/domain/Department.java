package com.brainserve.appointment.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID branchId;
    private UUID parentId;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private boolean active;
    @Version
    private long version;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Department() {
    }

    public Department(UUID branchId, UUID parentId, String code, String name) {
        this.id = UUID.randomUUID();
        this.branchId = branchId;
        this.parentId = parentId;
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public UUID getParentId() { return parentId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
