package com.brainserve.appointment.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token_sessions")
public class RefreshTokenSession {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID familyId;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant revokedAt;
    private UUID replacedBy;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant lastUsedAt;
    private String userAgent;
    private String ipAddress;

    protected RefreshTokenSession() {
    }

    public RefreshTokenSession(
            UUID familyId,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            String userAgent,
            String ipAddress) {
        this.id = UUID.randomUUID();
        this.familyId = familyId == null ? UUID.randomUUID() : familyId;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.userAgent = truncate(userAgent, 512);
        this.ipAddress = truncate(ipAddress, 64);
    }

    public boolean active() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void rotateTo(UUID replacementId) {
        this.revokedAt = Instant.now();
        this.lastUsedAt = Instant.now();
        this.replacedBy = replacementId;
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    private static String truncate(String value, int maximum) {
        if (value == null) return null;
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    public UUID getId() { return id; }
    public UUID getFamilyId() { return familyId; }
    public UUID getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
