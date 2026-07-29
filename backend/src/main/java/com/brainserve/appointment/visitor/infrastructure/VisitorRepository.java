package com.brainserve.appointment.visitor.infrastructure;

import com.brainserve.appointment.visitor.domain.Visitor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorRepository extends JpaRepository<Visitor, UUID> {
}
