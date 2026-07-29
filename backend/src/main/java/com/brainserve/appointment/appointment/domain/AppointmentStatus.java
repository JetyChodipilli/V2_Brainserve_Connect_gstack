package com.brainserve.appointment.appointment.domain;

import com.brainserve.appointment.shared.api.DomainException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.http.HttpStatus;

public enum AppointmentStatus {
    DRAFT,
    PENDING_VERIFICATION,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    RESCHEDULE_REQUESTED,
    RESCHEDULED,
    CANCELLED,
    CHECKED_IN,
    IN_MEETING,
    CHECKED_OUT,
    COMPLETED,
    NO_SHOW,
    EXPIRED;

    private static final Map<AppointmentStatus, EnumSet<AppointmentStatus>> ALLOWED = new EnumMap<>(AppointmentStatus.class);

    static {
        ALLOWED.put(DRAFT, EnumSet.of(PENDING_VERIFICATION, CANCELLED));
        ALLOWED.put(PENDING_VERIFICATION, EnumSet.of(PENDING_APPROVAL, CANCELLED, EXPIRED));
        ALLOWED.put(PENDING_APPROVAL, EnumSet.of(APPROVED, REJECTED, RESCHEDULE_REQUESTED, CANCELLED, EXPIRED));
        ALLOWED.put(APPROVED, EnumSet.of(RESCHEDULE_REQUESTED, CANCELLED, CHECKED_IN, NO_SHOW, EXPIRED));
        ALLOWED.put(REJECTED, EnumSet.noneOf(AppointmentStatus.class));
        ALLOWED.put(RESCHEDULE_REQUESTED, EnumSet.of(RESCHEDULED, REJECTED, CANCELLED));
        ALLOWED.put(RESCHEDULED, EnumSet.of(PENDING_APPROVAL, APPROVED, CANCELLED));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(AppointmentStatus.class));
        ALLOWED.put(CHECKED_IN, EnumSet.of(IN_MEETING, CHECKED_OUT));
        ALLOWED.put(IN_MEETING, EnumSet.of(CHECKED_OUT));
        ALLOWED.put(CHECKED_OUT, EnumSet.of(COMPLETED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(AppointmentStatus.class));
        ALLOWED.put(NO_SHOW, EnumSet.noneOf(AppointmentStatus.class));
        ALLOWED.put(EXPIRED, EnumSet.noneOf(AppointmentStatus.class));
    }

    public void requireTransitionTo(AppointmentStatus target) {
        if (!ALLOWED.getOrDefault(this, EnumSet.noneOf(AppointmentStatus.class)).contains(target)) {
            throw new DomainException(
                    "APPOINTMENT_INVALID_TRANSITION",
                    HttpStatus.CONFLICT,
                    "Appointment status cannot transition from " + this + " to " + target + ".");
        }
    }
}
