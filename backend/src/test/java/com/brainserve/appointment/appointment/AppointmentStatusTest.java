package com.brainserve.appointment.appointment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brainserve.appointment.appointment.domain.AppointmentStatus;
import com.brainserve.appointment.shared.api.DomainException;
import org.junit.jupiter.api.Test;

class AppointmentStatusTest {

    @Test
    void permitsTheOperationalHappyPath() {
        assertThatCode(() -> AppointmentStatus.PENDING_VERIFICATION
                .requireTransitionTo(AppointmentStatus.PENDING_APPROVAL)).doesNotThrowAnyException();
        assertThatCode(() -> AppointmentStatus.PENDING_APPROVAL
                .requireTransitionTo(AppointmentStatus.APPROVED)).doesNotThrowAnyException();
        assertThatCode(() -> AppointmentStatus.APPROVED
                .requireTransitionTo(AppointmentStatus.CHECKED_IN)).doesNotThrowAnyException();
        assertThatCode(() -> AppointmentStatus.CHECKED_IN
                .requireTransitionTo(AppointmentStatus.CHECKED_OUT)).doesNotThrowAnyException();
        assertThatCode(() -> AppointmentStatus.CHECKED_OUT
                .requireTransitionTo(AppointmentStatus.COMPLETED)).doesNotThrowAnyException();
    }

    @Test
    void rejectsARepeatedCheckIn() {
        assertThatThrownBy(() -> AppointmentStatus.CHECKED_IN
                .requireTransitionTo(AppointmentStatus.CHECKED_IN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("cannot transition");
    }

    @Test
    void rejectsReopeningACompletedAppointment() {
        assertThatThrownBy(() -> AppointmentStatus.COMPLETED
                .requireTransitionTo(AppointmentStatus.PENDING_APPROVAL))
                .isInstanceOf(DomainException.class);
    }
}
