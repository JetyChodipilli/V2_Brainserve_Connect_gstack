package com.brainserve.appointment.configuration.application;

import com.brainserve.appointment.employee.application.EmployeeService;
import com.brainserve.appointment.iam.application.BootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class SystemBootstrapCoordinator implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SystemBootstrapCoordinator.class);
    private final BootstrapProperties bootstrap;
    private final EmployeeService employees;

    public SystemBootstrapCoordinator(BootstrapProperties bootstrap, EmployeeService employees) {
        this.bootstrap = bootstrap;
        this.employees = employees;
    }

    @Override
    public void run(ApplicationArguments args) {
        employees.bootstrapCeo(bootstrap.ceoEmail(), bootstrap.ceoPassword());
        if (bootstrap.ceoEmail() != null && !bootstrap.ceoEmail().isBlank()) {
            log.info("CEO bootstrap verified with an employee profile and approval ownership");
        }
    }
}
