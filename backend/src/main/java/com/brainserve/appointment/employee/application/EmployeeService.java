package com.brainserve.appointment.employee.application;

import com.brainserve.appointment.employee.domain.Employee;
import com.brainserve.appointment.employee.domain.EmployeeStatus;
import com.brainserve.appointment.employee.infrastructure.EmployeeRepository;
import com.brainserve.appointment.iam.application.IdentityDirectory;
import com.brainserve.appointment.notification.application.OutboxService;
import com.brainserve.appointment.organization.application.OrganizationService;
import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.shared.audit.AuditService;
import com.brainserve.appointment.shared.security.CurrentUser;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {
    private static final String TEMPORARY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";
    private final EmployeeRepository employees;
    private final OrganizationService organization;
    private final IdentityDirectory identity;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final OutboxService outbox;
    private final SecureRandom random = new SecureRandom();

    public EmployeeService(
            EmployeeRepository employees,
            OrganizationService organization,
            IdentityDirectory identity,
            CurrentUser currentUser,
            AuditService audit,
            OutboxService outbox) {
        this.employees = employees;
        this.organization = organization;
        this.identity = identity;
        this.currentUser = currentUser;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public CreatedEmployee create(CreateEmployee command) {
        if (employees.existsByOfficialEmailIgnoreCase(command.officialEmail())
                || identity.loginExists(command.officialEmail())) {
            throw new DomainException("EMPLOYEE_EMAIL_EXISTS", HttpStatus.CONFLICT, "Official email already exists.");
        }
        organization.requireValidAssignment(command.branchId(), command.departmentId(), command.designationId());
        if (command.managerId() != null) requireActiveManager(command.managerId());
        String number = "BSPL-" + Year.now().getValue() + "-" + String.format("%04d", employees.nextBusinessSequence());
        Employee employee = employees.save(new Employee(
                number,
                command.firstName(),
                command.middleName(),
                command.lastName(),
                command.officialEmail(),
                command.personalEmail(),
                command.phone(),
                command.departmentId(),
                command.designationId(),
                command.branchId(),
                command.managerId(),
                command.employmentType(),
                command.joiningDate(),
                command.workLocation()));
        String temporaryPassword = temporaryPassword();
        identity.provisionEmployee(
                employee.getId(),
                employee.getOfficialEmail(),
                employee.getDisplayName(),
                temporaryPassword);
        audit.record(currentUser.id(), "CREATE", "EMPLOYEE", employee.getId(), null, "CONFIDENTIAL");
        outbox.publish("EMPLOYEE", employee.getId(), "EmployeeCreated",
                "{\"employeeId\":\"" + employee.getId() + "\",\"email\":\"" + employee.getOfficialEmail() + "\"}");
        return new CreatedEmployee(toView(employee), temporaryPassword);
    }

    @Transactional
    public void bootstrapCeo(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("Bootstrap passwords must contain at least 12 characters");
        }
        if (employees.existsByOfficialEmailIgnoreCase(email) || identity.loginExists(email)) {
            return;
        }
        OrganizationService.OrganizationAssignment assignment = organization.executiveAssignment();
        String number = "BSPL-" + Year.now().getValue() + "-" + String.format("%04d", employees.nextBusinessSequence());
        Employee ceo = employees.save(new Employee(
                number,
                "BrainServe",
                null,
                "CEO",
                email,
                null,
                null,
                assignment.departmentId(),
                assignment.designationId(),
                assignment.branchId(),
                null,
                "FULL_TIME",
                LocalDate.now(),
                "Hyderabad"));
        ceo.changeStatus(EmployeeStatus.ACTIVE);
        identity.provisionPrivilegedEmployee(
                ceo.getId(), ceo.getOfficialEmail(), ceo.getDisplayName(), password, "ROLE_CEO");
        outbox.publish("EMPLOYEE", ceo.getId(), "CeoBootstrapCompleted",
                "{\"employeeId\":\"" + ceo.getId() + "\",\"email\":\"" + ceo.getOfficialEmail() + "\"}");
    }

    @Transactional(readOnly = true)
    public List<EmployeeView> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return employees.findAllByOrderByDisplayName(PageRequest.of(Math.max(page, 0), safeSize))
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeView> activeHosts() {
        return employees.findAllByStatusOrderByDisplayName(EmployeeStatus.ACTIVE).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeView byId(UUID id) {
        Employee employee = find(id);
        if (!currentUser.has("EMPLOYEE_READ")) {
            UUID ownEmployee = identity.optionalEmployeeIdForUser(currentUser.id());
            if (!id.equals(ownEmployee)) throw new AccessDeniedException("Own profile only");
        }
        return toView(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeView me() {
        UUID employeeId = identity.employeeIdForUser(currentUser.id());
        return toView(find(employeeId));
    }

    @Transactional(readOnly = true)
    public DirectoryEmployee publicHost(UUID employeeId) {
        Employee employee = find(employeeId);
        if (!employee.activeHost()) {
            throw new DomainException("HOST_UNAVAILABLE", HttpStatus.CONFLICT, "Selected host is unavailable.");
        }
        return directory(employee);
    }

    @Transactional(readOnly = true)
    public DirectoryEmployee directoryById(UUID employeeId) {
        return directory(find(employeeId));
    }

    @Transactional(readOnly = true)
    public void requireExists(UUID employeeId) {
        if (!employees.existsById(employeeId)) {
            throw new DomainException("EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND, "Employee was not found.");
        }
    }

    @Transactional
    public EmployeeView changeStatus(UUID id, EmployeeStatus target) {
        Employee employee = find(id);
        EmployeeStatus previous = employee.getStatus();
        employee.changeStatus(target);
        audit.record(currentUser.id(), "STATUS_CHANGE", "EMPLOYEE", id, previous + " -> " + target, "CONFIDENTIAL");
        outbox.publish("EMPLOYEE", id, "EmployeeStatusChanged",
                "{\"employeeId\":\"" + id + "\",\"status\":\"" + target + "\"}");
        return toView(employee);
    }

    @Transactional
    public EmployeeView assignManager(UUID id, UUID managerId) {
        Employee employee = find(id);
        requireActiveManager(managerId);
        UUID cursor = managerId;
        for (int depth = 0; depth < 100 && cursor != null; depth++) {
            if (cursor.equals(id)) {
                throw new DomainException("CIRCULAR_MANAGER", HttpStatus.CONFLICT, "Reporting relationship would be circular.");
            }
            cursor = employees.findById(cursor).map(Employee::getManagerId).orElse(null);
        }
        employee.assignManager(managerId);
        audit.record(currentUser.id(), "MANAGER_CHANGE", "EMPLOYEE", id, null, "CONFIDENTIAL");
        outbox.publish("EMPLOYEE", id, "EmployeeManagerChanged",
                "{\"employeeId\":\"" + id + "\",\"managerId\":\"" + managerId + "\"}");
        return toView(employee);
    }

    private Employee find(UUID id) {
        return employees.findById(id)
                .orElseThrow(() -> new DomainException("EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND, "Employee was not found."));
    }

    private void requireActiveManager(UUID managerId) {
        if (!find(managerId).activeHost()) {
            throw new DomainException("MANAGER_INACTIVE", HttpStatus.CONFLICT, "Reporting manager must be active.");
        }
    }

    private String temporaryPassword() {
        StringBuilder value = new StringBuilder(18);
        for (int i = 0; i < 18; i++) {
            value.append(TEMPORARY_ALPHABET.charAt(random.nextInt(TEMPORARY_ALPHABET.length())));
        }
        return value.toString();
    }

    private EmployeeView toView(Employee employee) {
        return new EmployeeView(
                employee.getId(), employee.getEmployeeNumber(), employee.getDisplayName(),
                employee.getOfficialEmail(), employee.getPhone(), employee.getDepartmentId(),
                employee.getDesignationId(), employee.getBranchId(), employee.getManagerId(),
                employee.getEmploymentType(), employee.getJoiningDate(), employee.getStatus(),
                employee.getWorkLocation(), employee.getVersion());
    }

    private DirectoryEmployee directory(Employee employee) {
        return new DirectoryEmployee(
                employee.getId(), employee.getEmployeeNumber(), employee.getDisplayName(),
                employee.getDepartmentId(), employee.getDesignationId(), employee.getBranchId());
    }

    public record CreateEmployee(
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
    }

    public record EmployeeView(
            UUID id,
            String employeeNumber,
            String displayName,
            String officialEmail,
            String phone,
            UUID departmentId,
            UUID designationId,
            UUID branchId,
            UUID managerId,
            String employmentType,
            LocalDate joiningDate,
            EmployeeStatus status,
            String workLocation,
            long version) {
    }

    public record CreatedEmployee(EmployeeView employee, String temporaryPassword) {
    }

    public record DirectoryEmployee(
            UUID id,
            String employeeNumber,
            String displayName,
            UUID departmentId,
            UUID designationId,
            UUID branchId) {
    }
}
