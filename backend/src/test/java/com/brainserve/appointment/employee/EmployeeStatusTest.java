package com.brainserve.appointment.employee;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brainserve.appointment.employee.domain.EmployeeStatus;
import com.brainserve.appointment.shared.api.DomainException;
import org.junit.jupiter.api.Test;

class EmployeeStatusTest {

    @Test
    void supportsOnboardingToActive() {
        assertThatCode(() -> EmployeeStatus.ONBOARDING.requireTransitionTo(EmployeeStatus.ACTIVE))
                .doesNotThrowAnyException();
    }

    @Test
    void preventsReactivatingAnInactiveRecord() {
        assertThatThrownBy(() -> EmployeeStatus.INACTIVE.requireTransitionTo(EmployeeStatus.ACTIVE))
                .isInstanceOf(DomainException.class);
    }
}
