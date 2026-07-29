package com.brainserve.appointment.employee.domain;

import com.brainserve.appointment.shared.api.DomainException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.http.HttpStatus;

public enum EmployeeStatus {
    DRAFT,
    ONBOARDING,
    ACTIVE,
    ON_LEAVE,
    NOTICE_PERIOD,
    SUSPENDED,
    RESIGNED,
    TERMINATED,
    INACTIVE;

    private static final Map<EmployeeStatus, EnumSet<EmployeeStatus>> ALLOWED = new EnumMap<>(EmployeeStatus.class);

    static {
        ALLOWED.put(DRAFT, EnumSet.of(ONBOARDING, INACTIVE));
        ALLOWED.put(ONBOARDING, EnumSet.of(ACTIVE, INACTIVE));
        ALLOWED.put(ACTIVE, EnumSet.of(ON_LEAVE, NOTICE_PERIOD, SUSPENDED, RESIGNED, TERMINATED, INACTIVE));
        ALLOWED.put(ON_LEAVE, EnumSet.of(ACTIVE, NOTICE_PERIOD, RESIGNED));
        ALLOWED.put(NOTICE_PERIOD, EnumSet.of(ACTIVE, RESIGNED, TERMINATED));
        ALLOWED.put(SUSPENDED, EnumSet.of(ACTIVE, TERMINATED, INACTIVE));
        ALLOWED.put(RESIGNED, EnumSet.of(INACTIVE));
        ALLOWED.put(TERMINATED, EnumSet.of(INACTIVE));
        ALLOWED.put(INACTIVE, EnumSet.noneOf(EmployeeStatus.class));
    }

    public void requireTransitionTo(EmployeeStatus target) {
        if (!ALLOWED.getOrDefault(this, EnumSet.noneOf(EmployeeStatus.class)).contains(target)) {
            throw new DomainException(
                    "EMPLOYEE_INVALID_TRANSITION",
                    HttpStatus.CONFLICT,
                    "Employee status cannot transition from " + this + " to " + target + ".");
        }
    }
}
