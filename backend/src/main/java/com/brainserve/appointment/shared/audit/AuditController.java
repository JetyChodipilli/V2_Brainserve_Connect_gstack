package com.brainserve.appointment.shared.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
@PreAuthorize("hasAuthority('AUDIT_VIEW')")
public class AuditController {
    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    List<AuditView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return repository.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))
                .map(AuditView::from).toList();
    }

    @GetMapping("/{id}")
    AuditView byId(@PathVariable UUID id) {
        return repository.findById(id).map(AuditView::from)
                .orElseThrow(() -> new com.brainserve.appointment.shared.api.DomainException(
                        "AUDIT_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND, "Audit event was not found."));
    }

    record AuditView(
            UUID id,
            String eventType,
            UUID actorUserId,
            String action,
            String entityType,
            String entityId,
            String reason,
            String correlationId,
            String sensitivity,
            Instant occurredAt) {
        static AuditView from(AuditEvent event) {
            return new AuditView(event.getId(), event.getEventType(), event.getActorUserId(), event.getAction(),
                    event.getEntityType(), event.getEntityId(), event.getReason(), event.getCorrelationId(),
                    event.getSensitivity(), event.getOccurredAt());
        }
    }
}
