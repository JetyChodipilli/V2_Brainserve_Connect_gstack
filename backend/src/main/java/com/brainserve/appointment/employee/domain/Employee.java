package com.brainserve.appointment.employee.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String employeeNumber;
    @Column(nullable = false)
    private String firstName;
    private String middleName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private String displayName;
    @Column(nullable = false, unique = true)
    private String officialEmail;
    private String personalEmail;
    private String phone;
    @Column(nullable = false)
    private UUID departmentId;
    @Column(nullable = false)
    private UUID designationId;
    @Column(nullable = false)
    private UUID branchId;
    private UUID managerId;
    @Column(nullable = false)
    private String employmentType;
    @Column(nullable = false)
    private LocalDate joiningDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status;
    private String workLocation;
    @Version
    private long version;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Employee() {
    }

    public Employee(
            String employeeNumber,
            String firstName,
            String middleName,
            String lastName,
            String officialEmail,
            String personalEmail,
            String phone,
            UUID departmentId,
            UUID designationId,
            UUID branchId,
            UUID managerId,
            String employmentType,
            LocalDate joiningDate,
            String workLocation) {
        this.id = UUID.randomUUID();
        this.employeeNumber = employeeNumber;
        this.firstName = firstName.trim();
        this.middleName = blankToNull(middleName);
        this.lastName = lastName.trim();
        this.displayName = (this.firstName + " " + this.lastName).trim();
        this.officialEmail = officialEmail.trim().toLowerCase();
        this.personalEmail = personalEmail == null ? null : personalEmail.trim().toLowerCase();
        this.phone = blankToNull(phone);
        this.departmentId = departmentId;
        this.designationId = designationId;
        this.branchId = branchId;
        this.managerId = managerId;
        this.employmentType = employmentType;
        this.joiningDate = joiningDate;
        this.status = EmployeeStatus.ONBOARDING;
        this.workLocation = blankToNull(workLocation);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void changeStatus(EmployeeStatus target) {
        status.requireTransitionTo(target);
        status = target;
        updatedAt = Instant.now();
    }

    public void assignManager(UUID managerId) {
        if (id.equals(managerId)) {
            throw new IllegalArgumentException("Employee cannot report to themselves");
        }
        this.managerId = managerId;
        updatedAt = Instant.now();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public String getEmployeeNumber() { return employeeNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDisplayName() { return displayName; }
    public String getOfficialEmail() { return officialEmail; }
    public String getPhone() { return phone; }
    public UUID getDepartmentId() { return departmentId; }
    public UUID getDesignationId() { return designationId; }
    public UUID getBranchId() { return branchId; }
    public UUID getManagerId() { return managerId; }
    public String getEmploymentType() { return employmentType; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public EmployeeStatus getStatus() { return status; }
    public String getWorkLocation() { return workLocation; }
    public long getVersion() { return version; }
    public boolean activeHost() { return status == EmployeeStatus.ACTIVE || status == EmployeeStatus.ONBOARDING; }
}
