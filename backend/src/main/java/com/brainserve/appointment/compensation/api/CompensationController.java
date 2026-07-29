package com.brainserve.appointment.compensation.api;

import com.brainserve.appointment.compensation.application.CompensationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CompensationController {
    private final CompensationService service;

    public CompensationController(CompensationService service) {
        this.service = service;
    }

    @PostMapping("/employees/{employeeId}/compensation/change-requests")
    @PreAuthorize("hasAuthority('SALARY_WRITE')")
    CompensationService.CompensationView propose(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CompensationRequest request) {
        return service.propose(employeeId, request.command());
    }

    @GetMapping("/employees/{employeeId}/compensation/current")
    @PreAuthorize("hasAuthority('SALARY_READ')")
    CompensationService.CompensationView current(@PathVariable UUID employeeId) {
        return service.current(employeeId);
    }

    @GetMapping("/employees/{employeeId}/compensation/history")
    @PreAuthorize("hasAuthority('SALARY_READ')")
    List<CompensationService.CompensationView> history(@PathVariable UUID employeeId) {
        return service.history(employeeId);
    }

    @GetMapping("/compensation/change-requests")
    @PreAuthorize("hasAuthority('SALARY_APPROVE')")
    List<CompensationService.CompensationView> pending() {
        return service.pending();
    }

    @PostMapping("/compensation/change-requests/{id}/approve")
    @PreAuthorize("hasAuthority('SALARY_APPROVE')")
    CompensationService.CompensationView approve(@PathVariable UUID id) {
        return service.decide(id, true);
    }

    @PostMapping("/compensation/change-requests/{id}/reject")
    @PreAuthorize("hasAuthority('SALARY_APPROVE')")
    CompensationService.CompensationView reject(@PathVariable UUID id) {
        return service.decide(id, false);
    }

    record CompensationRequest(
            @NotNull @DecimalMin("0.00") BigDecimal basic,
            @NotNull @DecimalMin("0.00") BigDecimal hra,
            @NotNull @DecimalMin("0.00") BigDecimal allowances,
            @NotNull @DecimalMin("0.00") BigDecimal deductions,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo) {
        CompensationService.CompensationCommand command() {
            return new CompensationService.CompensationCommand(
                    basic, hra, allowances, deductions, currency, effectiveFrom, effectiveTo);
        }
    }
}
