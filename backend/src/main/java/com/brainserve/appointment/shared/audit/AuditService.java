package com.brainserve.appointment.shared.audit;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public void record(UUID actor, String action, String entityType, Object entityId, String reason, String sensitivity) {
        repository.save(new AuditEvent(
                actor,
                action,
                entityType,
                entityId == null ? null : entityId.toString(),
                reason,
                sensitivity));
    }
}
