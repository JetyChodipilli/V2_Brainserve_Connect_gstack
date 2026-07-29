package com.brainserve.appointment.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    private UUID id;
    private UUID employeeId;
    @Column(nullable = false, unique = true)
    private String login;
    @Column(nullable = false)
    private String displayName;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private boolean enabled;
    private Instant lockedUntil;
    @Column(nullable = false)
    private int failedAttempts;
    @Column(nullable = false)
    private boolean mustChangePassword;
    private Instant passwordChangedAt;
    @Version
    private long version;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected UserAccount() {
    }

    public static UserAccount bootstrap(String login, String displayName, String passwordHash, Role role) {
        UserAccount user = new UserAccount();
        user.id = UUID.randomUUID();
        user.login = login.trim().toLowerCase();
        user.displayName = displayName;
        user.passwordHash = passwordHash;
        user.enabled = true;
        user.mustChangePassword = true;
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        user.roles.add(role);
        return user;
    }

    public static UserAccount forEmployee(
            UUID employeeId,
            String login,
            String displayName,
            String passwordHash,
            Role employeeRole) {
        UserAccount user = bootstrap(login, displayName, passwordHash, employeeRole);
        user.employeeId = employeeId;
        return user;
    }

    public void failedLogin(int maximum, Instant lockedUntil) {
        failedAttempts += 1;
        if (failedAttempts >= maximum) {
            this.lockedUntil = lockedUntil;
        }
        updatedAt = Instant.now();
    }

    public void successfulLogin() {
        failedAttempts = 0;
        lockedUntil = null;
        updatedAt = Instant.now();
    }

    public void changePassword(String newHash) {
        passwordHash = newHash;
        mustChangePassword = false;
        passwordChangedAt = Instant.now();
        updatedAt = Instant.now();
    }

    public void replaceRoles(Set<Role> replacement) {
        if (replacement == null || replacement.isEmpty()) {
            throw new IllegalArgumentException("A user must have at least one role");
        }
        roles.clear();
        roles.addAll(replacement);
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public String getLogin() { return login; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public Instant getLockedUntil() { return lockedUntil; }
    public boolean isLocked() { return lockedUntil != null && lockedUntil.isAfter(Instant.now()); }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }
}
