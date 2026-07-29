package com.brainserve.appointment.employee.api;

import com.brainserve.appointment.employee.application.EmployeeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicHostController {
    private final EmployeeService employees;

    public PublicHostController(EmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping("/hosts")
    List<PublicHost> hosts() {
        return employees.activeHosts().stream()
                .map(host -> new PublicHost(host.id(), host.displayName(), host.departmentId(), host.designationId()))
                .toList();
    }

    record PublicHost(java.util.UUID id, String displayName, java.util.UUID departmentId, java.util.UUID designationId) {
    }
}
