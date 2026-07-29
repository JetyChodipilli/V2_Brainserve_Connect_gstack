package com.brainserve.appointment.reception.infrastructure;

import com.brainserve.appointment.reception.domain.VisitAccessRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessRecordRepository extends JpaRepository<VisitAccessRecord, UUID> {
    boolean existsByAppointmentId(UUID appointmentId);
    Optional<VisitAccessRecord> findByAppointmentId(UUID appointmentId);
    List<VisitAccessRecord> findAllByCheckedInAtIsNotNullAndCheckedOutAtIsNullOrderByCheckedInAt();
}
