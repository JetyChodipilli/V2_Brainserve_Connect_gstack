package com.brainserve.appointment.notification.application;

import com.brainserve.appointment.notification.domain.OutboxEvent;
import com.brainserve.appointment.notification.infrastructure.OutboxRepository;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
    private final OutboxRepository repository;

    public OutboxService(OutboxRepository repository) {
        this.repository = repository;
    }

    public void publish(String aggregateType, Object aggregateId, String eventType, String payloadJson) {
        repository.save(new OutboxEvent(aggregateType, aggregateId, eventType, payloadJson));
    }
}
