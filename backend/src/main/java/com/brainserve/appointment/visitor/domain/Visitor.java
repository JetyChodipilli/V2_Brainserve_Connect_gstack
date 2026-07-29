package com.brainserve.appointment.visitor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visitors")
public class Visitor {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String phone;
    private String company;
    @Column(nullable = false)
    private String verificationStatus;
    @Column(nullable = false)
    private boolean restricted;
    private String restrictionReason;
    @Column(nullable = false)
    private String consentVersion;
    @Column(nullable = false)
    private Instant consentedAt;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Visitor() {
    }

    public Visitor(String firstName, String lastName, String email, String phone, String company, String consentVersion) {
        this.id = UUID.randomUUID();
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.email = email.trim().toLowerCase();
        this.phone = normalizePhone(phone);
        this.company = company == null || company.isBlank() ? null : company.trim();
        this.verificationStatus = "UNVERIFIED";
        this.consentVersion = consentVersion;
        this.consentedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void otpVerified() {
        verificationStatus = "OTP_VERIFIED";
        updatedAt = Instant.now();
    }

    public void identityVerified() {
        verificationStatus = "IDENTITY_VERIFIED";
        updatedAt = Instant.now();
    }

    private static String normalizePhone(String value) {
        String normalized = value.replaceAll("[^+0-9]", "");
        return normalized.startsWith("+") ? normalized : "+" + normalized;
    }

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDisplayName() { return firstName + " " + lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCompany() { return company; }
    public String getVerificationStatus() { return verificationStatus; }
    public boolean isRestricted() { return restricted; }
    public String getRestrictionReason() { return restrictionReason; }
}
