package com.brainserve.appointment.employee.api;

import com.brainserve.appointment.employee.application.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class AccountAdminController {
    private final EmployeeService employees;

    public AccountAdminController(EmployeeService employees) {
        this.employees = employees;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EmployeeService.CreatedAccount create(@Valid @RequestBody CreateAccountRequest request) {
        return employees.createAccount(request.toCommand(), request.role());
    }

    record CreateAccountRequest(
            @NotBlank @Size(max = 80) String firstName,
            @Size(max = 80) String middleName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 180) String officialEmail,
            @Email @Size(max = 180) String personalEmail,
            @Size(max = 30) String phone,
            @NotNull UUID departmentId,
            @NotNull UUID designationId,
            @NotNull UUID branchId,
            UUID managerId,
            @NotBlank @Size(max = 30) String employmentType,
            @NotNull LocalDate joiningDate,
            @Size(max = 160) String workLocation,
            @NotBlank
            @Pattern(
                    regexp = "ROLE_(EMPLOYEE|SECURITY|RECEPTIONIST)",
                    message = "Role must be ROLE_EMPLOYEE, ROLE_SECURITY, or ROLE_RECEPTIONIST")
            String role) {

        EmployeeService.CreateEmployee toCommand() {
            return new EmployeeService.CreateEmployee(
                    firstName,
                    middleName,
                    lastName,
                    officialEmail,
                    personalEmail,
                    phone,
                    departmentId,
                    designationId,
                    branchId,
                    managerId,
                    employmentType,
                    joiningDate,
                    workLocation);
        }
    }
}