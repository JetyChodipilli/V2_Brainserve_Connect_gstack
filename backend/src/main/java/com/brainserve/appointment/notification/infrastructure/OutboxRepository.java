package com.brainserve.appointment.notification.infrastructure;

import com.brainserve.appointment.notification.domain.OutboxEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
}
