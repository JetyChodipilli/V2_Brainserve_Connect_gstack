package com.brainserve.appointment.compensation.domain;

import com.brainserve.appointment.shared.api.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "compensation_packages")
public class CompensationPackage {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID employeeId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basic;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal hra;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal allowances;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal deductions;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal gross;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal net;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal annualCtc;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false)
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private UUID proposedBy;
    private UUID approvedBy;
    @Version
    private long version;
    @Column(nullable = false)
    private Instant createdAt;

    protected CompensationPackage() {
    }

    public CompensationPackage(
            UUID employeeId,
            BigDecimal basic,
            BigDecimal hra,
            BigDecimal allowances,
            BigDecimal deductions,
            String currency,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            UUID proposedBy) {
        this.id = UUID.randomUUID();
        this.employeeId = employeeId;
        this.basic = money(basic);
        this.hra = money(hra);
        this.allowances = money(allowances);
        this.deductions = money(deductions);
        this.gross = money(this.basic.add(this.hra).add(this.allowances));
        this.net = money(this.gross.subtract(this.deductions));
        if (net.signum() < 0) throw new IllegalArgumentException("Deductions cannot exceed gross salary");
        this.annualCtc = money(this.gross.multiply(BigDecimal.valueOf(12)));
        this.currency = currency.toUpperCase();
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = "PROPOSED";
        this.proposedBy = proposedBy;
        this.createdAt = Instant.now();
    }

    public void approve(UUID actor) {
        if (!status.equals("PROPOSED")) {
            throw new DomainException("SALARY_REQUEST_FINAL", HttpStatus.CONFLICT, "Salary request is already final.");
        }
        if (proposedBy.equals(actor)) {
            throw new DomainException("MAKER_CHECKER_REQUIRED", HttpStatus.FORBIDDEN, "Proposer cannot approve their own salary request.");
        }
        status = "APPROVED";
        approvedBy = actor;
    }

    public void reject(UUID actor) {
        if (!status.equals("PROPOSED")) {
            throw new DomainException("SALARY_REQUEST_FINAL", HttpStatus.CONFLICT, "Salary request is already final.");
        }
        status = "REJECTED";
        approvedBy = actor;
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("Money values must be non-negative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public BigDecimal getBasic() { return basic; }
    public BigDecimal getHra() { return hra; }
    public BigDecimal getAllowances() { return allowances; }
    public BigDecimal getDeductions() { return deductions; }
    public BigDecimal getGross() { return gross; }
    public BigDecimal getNet() { return net; }
    public BigDecimal getAnnualCtc() { return annualCtc; }
    public String getCurrency() { return currency; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public String getStatus() { return status; }
    public UUID getProposedBy() { return proposedBy; }
    public UUID getApprovedBy() { return approvedBy; }
    public long getVersion() { return version; }
}
