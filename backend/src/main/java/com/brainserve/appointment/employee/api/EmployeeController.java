package com.brainserve.appointment.employee.api;

import com.brainserve.appointment.employee.application.EmployeeService;
import com.brainserve.appointment.employee.domain.EmployeeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    EmployeeService.CreatedEmployee create(@Valid @RequestBody EmployeeRequest request) {
        return service.create(request.toCommand());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    List<EmployeeService.EmployeeView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.list(page, size);
    }

    @GetMapping("/hosts")
    List<EmployeeService.EmployeeView> hosts() {
        return service.activeHosts();
    }

    @GetMapping("/me")
    EmployeeService.EmployeeView me() {
        return service.me();
    }

    @GetMapping("/{id}")
    EmployeeService.EmployeeView byId(@PathVariable UUID id) {
        return service.byId(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_STATUS_CHANGE')")
    EmployeeService.EmployeeView status(@PathVariable UUID id, @RequestBody @Valid StatusRequest request) {
        return service.changeStatus(id, request.status());
    }

    @PatchMapping("/{id}/manager")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    EmployeeService.EmployeeView manager(@PathVariable UUID id, @RequestBody @Valid ManagerRequest request) {
        return service.assignManager(id, request.managerId());
    }

    record EmployeeRequest(
            @NotBlank @Size(max = 80) String firstName,
            @Size(max = 80) String middleName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email String officialEmail,
            @Email String personalEmail,
            @Size(max = 30) String phone,
            @NotNull UUID departmentId,
            @NotNull UUID designationId,
            @NotNull UUID branchId,
            UUID managerId,
            @NotBlank @Size(max = 30) String employmentType,
            @NotNull LocalDate joiningDate,
            @Size(max = 160) String workLocation) {

        EmployeeService.CreateEmployee toCommand() {
            return new EmployeeService.CreateEmployee(firstName, middleName, lastName, officialEmail,
                    personalEmail, phone, departmentId, designationId, branchId, managerId,
                    employmentType, joiningDate, workLocation);
        }
    }

    record StatusRequest(@NotNull EmployeeStatus status) {
    }

    record ManagerRequest(@NotNull UUID managerId) {
    }
}
