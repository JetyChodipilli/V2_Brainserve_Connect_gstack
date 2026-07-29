package com.brainserve.appointment.appointment.infrastructure;

import com.brainserve.appointment.appointment.domain.Appointment;
import com.brainserve.appointment.appointment.domain.AppointmentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Optional<Appointment> findByReferenceNumber(String referenceNumber);
    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);
    List<Appointment> findAllByHostEmployeeIdAndStatusInOrderByStartsAt(
            UUID hostEmployeeId,
            Collection<AppointmentStatus> statuses);
    List<Appointment> findAllByStatusInAndStartsAtBetweenOrderByStartsAt(
            Collection<AppointmentStatus> statuses,
            Instant from,
            Instant to);
    List<Appointment> findAllByStatusOrderByStartsAt(AppointmentStatus status);
    boolean existsByHostEmployeeIdAndStartsAtAndStatusIn(
            UUID hostEmployeeId,
            Instant startsAt,
            Collection<AppointmentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Appointment> findLockedById(UUID id);
}
