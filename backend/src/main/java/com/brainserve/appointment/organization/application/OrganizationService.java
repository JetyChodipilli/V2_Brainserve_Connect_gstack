package com.brainserve.appointment.organization.application;

import com.brainserve.appointment.notification.application.OutboxService;
import com.brainserve.appointment.organization.domain.Branch;
import com.brainserve.appointment.organization.domain.Department;
import com.brainserve.appointment.organization.domain.Designation;
import com.brainserve.appointment.organization.infrastructure.BranchRepository;
import com.brainserve.appointment.organization.infrastructure.DepartmentRepository;
import com.brainserve.appointment.organization.infrastructure.DesignationRepository;
import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.shared.audit.AuditService;
import com.brainserve.appointment.shared.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
    private final BranchRepository branches;
    private final DepartmentRepository departments;
    private final DesignationRepository designations;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final OutboxService outbox;

    public OrganizationService(
            BranchRepository branches,
            DepartmentRepository departments,
            DesignationRepository designations,
            CurrentUser currentUser,
            AuditService audit,
            OutboxService outbox) {
        this.branches = branches;
        this.departments = departments;
        this.designations = designations;
        this.currentUser = currentUser;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public List<BranchView> branches() {
        return branches.findAll().stream().map(BranchView::from).toList();
    }

    @Transactional
    public BranchView createBranch(String code, String name, String city) {
        Branch branch = branches.save(new Branch(code, name, city));
        changed("BRANCH", branch.getId(), "BRANCH_CREATED");
        return BranchView.from(branch);
    }

    @Transactional(readOnly = true)
    public List<DepartmentView> departments() {
        return departments.findAllByActiveTrueOrderByName().stream().map(DepartmentView::from).toList();
    }

    @Transactional
    public DepartmentView createDepartment(UUID branchId, UUID parentId, String code, String name) {
        if (!branches.existsById(branchId)) {
            throw new DomainException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Branch was not found.");
        }
        if (departments.existsByCodeIgnoreCase(code)) {
            throw new DomainException("DEPARTMENT_CODE_EXISTS", HttpStatus.CONFLICT, "Department code already exists.");
        }
        if (parentId != null && !departments.existsById(parentId)) {
            throw new DomainException("PARENT_DEPARTMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Parent department was not found.");
        }
        Department department = departments.save(new Department(branchId, parentId, code, name));
        changed("DEPARTMENT", department.getId(), "DEPARTMENT_CREATED");
        return DepartmentView.from(department);
    }

    @Transactional(readOnly = true)
    public List<DesignationView> designations() {
        return designations.findAllByActiveTrueOrderByName().stream().map(DesignationView::from).toList();
    }

    @Transactional
    public DesignationView createDesignation(UUID departmentId, String name, int level) {
        if (departmentId != null && !departments.existsById(departmentId)) {
            throw new DomainException("DEPARTMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Department was not found.");
        }
        Designation designation = designations.save(new Designation(departmentId, name, level));
        changed("DESIGNATION", designation.getId(), "DESIGNATION_CREATED");
        return DesignationView.from(designation);
    }

    @Transactional(readOnly = true)
    public void requireValidAssignment(UUID branchId, UUID departmentId, UUID designationId) {
        if (!branches.existsById(branchId) || !departments.existsById(departmentId) || !designations.existsById(designationId)) {
            throw new DomainException("ORGANIZATION_REFERENCE_INVALID", HttpStatus.BAD_REQUEST,
                    "Branch, department, or designation is invalid.");
        }
    }

    @Transactional(readOnly = true)
    public OrganizationAssignment executiveAssignment() {
        Branch branch = branches.findByCodeIgnoreCase("HYD")
                .orElseThrow(() -> new IllegalStateException("Seed branch HYD is missing"));
        Department department = departments.findByCodeIgnoreCase("EXEC")
                .orElseThrow(() -> new IllegalStateException("Seed department EXEC is missing"));
        Designation designation = designations.findFirstByDepartmentIdOrderByLevelDesc(department.getId())
                .orElseThrow(() -> new IllegalStateException("Executive designation is missing"));
        return new OrganizationAssignment(branch.getId(), department.getId(), designation.getId());
    }

    private void changed(String type, UUID id, String event) {
        audit.record(currentUser.id(), "CREATE", type, id, null, "INTERNAL");
        outbox.publish(type, id, event, "{\"id\":\"" + id + "\"}");
    }

    public record BranchView(UUID id, String code, String name, String city, boolean active) {
        static BranchView from(Branch branch) {
            return new BranchView(branch.getId(), branch.getCode(), branch.getName(), branch.getCity(), branch.isActive());
        }
    }

    public record DepartmentView(UUID id, UUID branchId, UUID parentId, String code, String name, boolean active) {
        static DepartmentView from(Department department) {
            return new DepartmentView(department.getId(), department.getBranchId(), department.getParentId(),
                    department.getCode(), department.getName(), department.isActive());
        }
    }

    public record DesignationView(UUID id, UUID departmentId, String name, int level, boolean active) {
        static DesignationView from(Designation designation) {
            return new DesignationView(designation.getId(), designation.getDepartmentId(), designation.getName(),
                    designation.getLevel(), designation.isActive());
        }
    }

    public record OrganizationAssignment(UUID branchId, UUID departmentId, UUID designationId) {
    }
}
