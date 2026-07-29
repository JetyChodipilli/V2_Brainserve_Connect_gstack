package com.brainserve.appointment.compensation.application;

import com.brainserve.appointment.compensation.domain.CompensationPackage;
import com.brainserve.appointment.compensation.infrastructure.CompensationRepository;
import com.brainserve.appointment.employee.application.EmployeeService;
import com.brainserve.appointment.notification.application.OutboxService;
import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.shared.audit.AuditService;
import com.brainserve.appointment.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompensationService {
    private final CompensationRepository compensation;
    private final EmployeeService employees;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final OutboxService outbox;

    public CompensationService(
            CompensationRepository compensation,
            EmployeeService employees,
            CurrentUser currentUser,
            AuditService audit,
            OutboxService outbox) {
        this.compensation = compensation;
        this.employees = employees;
        this.currentUser = currentUser;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public CompensationView propose(UUID employeeId, CompensationCommand command) {
        employees.requireExists(employeeId);
        if (compensation.hasOverlap(employeeId, command.effectiveFrom(), command.effectiveTo())) {
            throw new DomainException("SALARY_PERIOD_OVERLAP", HttpStatus.CONFLICT, "Compensation period overlaps an existing request.");
        }
        CompensationPackage saved = compensation.save(new CompensationPackage(
                employeeId, command.basic(), command.hra(), command.allowances(), command.deductions(),
                command.currency(), command.effectiveFrom(), command.effectiveTo(), currentUser.id()));
        audit.record(currentUser.id(), "PROPOSE", "COMPENSATION", saved.getId(), null, "RESTRICTED");
        outbox.publish("COMPENSATION", saved.getId(), "SalaryChangeProposed",
                "{\"compensationId\":\"" + saved.getId() + "\"}");
        return view(saved);
    }

    @Transactional
    public CompensationView current(UUID employeeId) {
        CompensationPackage current = compensation.current(employeeId, LocalDate.now()).stream().findFirst()
                .orElseThrow(() -> new DomainException("COMPENSATION_NOT_FOUND", HttpStatus.NOT_FOUND, "Current compensation was not found."));
        audit.record(currentUser.id(), "READ", "COMPENSATION", current.getId(), "Current package", "RESTRICTED");
        return view(current);
    }

    @Transactional
    public List<CompensationView> history(UUID employeeId) {
        List<CompensationView> result = compensation.findAllByEmployeeIdOrderByEffectiveFromDesc(employeeId)
                .stream().map(this::view).toList();
        audit.record(currentUser.id(), "READ_HISTORY", "COMPENSATION", employeeId, null, "RESTRICTED");
        return result;
    }

    @Transactional
    public List<CompensationView> pending() {
        List<CompensationView> result = compensation.findAllByStatusOrderByCreatedAt("PROPOSED")
                .stream().map(this::view).toList();
        audit.record(currentUser.id(), "READ_PENDING", "COMPENSATION", null, "Pending review queue", "RESTRICTED");
        return result;
    }

    @Transactional
    public CompensationView decide(UUID id, boolean approve) {
        CompensationPackage item = compensation.findById(id)
                .orElseThrow(() -> new DomainException("COMPENSATION_NOT_FOUND", HttpStatus.NOT_FOUND, "Compensation request was not found."));
        if (approve) item.approve(currentUser.id()); else item.reject(currentUser.id());
        audit.record(currentUser.id(), approve ? "APPROVE" : "REJECT", "COMPENSATION", id, null, "RESTRICTED");
        outbox.publish("COMPENSATION", id, approve ? "SalaryChangeApproved" : "SalaryChangeRejected",
                "{\"compensationId\":\"" + id + "\"}");
        return view(item);
    }

    private CompensationView view(CompensationPackage item) {
        return new CompensationView(item.getId(), item.getEmployeeId(), item.getBasic(), item.getHra(),
                item.getAllowances(), item.getDeductions(), item.getGross(), item.getNet(), item.getAnnualCtc(),
                item.getCurrency(), item.getEffectiveFrom(), item.getEffectiveTo(), item.getStatus(),
                item.getProposedBy(), item.getApprovedBy(), item.getVersion());
    }

    public record CompensationCommand(
            BigDecimal basic,
            BigDecimal hra,
            BigDecimal allowances,
            BigDecimal deductions,
            String currency,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {
    }

    public record CompensationView(
            UUID id,
            UUID employeeId,
            BigDecimal basic,
            BigDecimal hra,
            BigDecimal allowances,
            BigDecimal deductions,
            BigDecimal gross,
            BigDecimal net,
            BigDecimal annualCtc,
            String currency,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String status,
            UUID proposedBy,
            UUID approvedBy,
            long version) {
    }
}
