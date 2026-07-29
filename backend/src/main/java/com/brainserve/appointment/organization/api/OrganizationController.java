package com.brainserve.appointment.organization.api;

import com.brainserve.appointment.organization.application.OrganizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrganizationController {
    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping("/branches")
    List<OrganizationService.BranchView> branches() {
        return service.branches();
    }

    @PostMapping("/branches")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    OrganizationService.BranchView createBranch(@Valid @RequestBody BranchRequest request) {
        return service.createBranch(request.code(), request.name(), request.city());
    }

    @GetMapping("/departments")
    List<OrganizationService.DepartmentView> departments() {
        return service.departments();
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    OrganizationService.DepartmentView createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return service.createDepartment(request.branchId(), request.parentId(), request.code(), request.name());
    }

    @GetMapping("/designations")
    List<OrganizationService.DesignationView> designations() {
        return service.designations();
    }

    @PostMapping("/designations")
    @PreAuthorize("hasAuthority('DESIGNATION_MANAGE')")
    OrganizationService.DesignationView createDesignation(@Valid @RequestBody DesignationRequest request) {
        return service.createDesignation(request.departmentId(), request.name(), request.level());
    }

    record BranchRequest(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 120) String city) {
    }

    record DepartmentRequest(
            @NotNull UUID branchId,
            UUID parentId,
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 160) String name) {
    }

    record DesignationRequest(
            UUID departmentId,
            @NotBlank @Size(max = 160) String name,
            @Min(1) @Max(20) int level) {
    }
}
